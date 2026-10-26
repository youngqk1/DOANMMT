package com.filesync.server;

import com.filesync.common.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Quản lý kho lưu trữ tệp tin trên máy chủ, lưu trữ phiên bản lịch sử (Versioning)
 * và phát hiện đụng độ (Conflict Detection) với an toàn đa luồng
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class ServerStorageManager {
    private final File rootDir;
    private final File versionsDir;
    private final File catalogFile;
    private final Map<String, FileMetadata> catalog = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ServerStorageManager(File storageDirectory) {
        this.rootDir = storageDirectory;
        this.versionsDir = new File(rootDir, Constants.VERSIONS_FOLDER);
        this.catalogFile = new File(rootDir, Constants.CATALOG_FILE);

        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }
        if (!versionsDir.exists()) {
            versionsDir.mkdirs();
        }

        loadCatalog();
    }

    public File getRootDir() {
        return rootDir;
    }

    /**
     * Tải danh mục metadata từ đĩa khi máy chủ khởi động
     */
    private void loadCatalog() {
        lock.writeLock().lock();
        try {
            if (catalogFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(catalogFile, Constants.DEFAULT_CHARSET))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        FileMetadata meta = FileMetadata.decode(line);
                        if (meta != null && !meta.isDeleted()) {
                            catalog.put(meta.getRelativePath(), meta);
                        }
                    }
                } catch (IOException e) {
                    System.err.println("Cảnh báo: Không thể đọc file catalog: " + e.getMessage());
                }
            }
            // Quét lại thư mục thực tế để cập nhật đồng bộ các tệp hiện hữu
            rescanPhysicalFiles();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lưu chỉ mục metadata xuống đĩa
     */
    private void persistCatalog() {
        lock.readLock().lock();
        try {
            List<String> lines = new ArrayList<>();
            for (FileMetadata meta : catalog.values()) {
                lines.add(meta.encode());
            }
            File tempFile = new File(rootDir, Constants.CATALOG_FILE + ".tmp");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile, Constants.DEFAULT_CHARSET))) {
                for (String l : lines) {
                    writer.write(l);
                    writer.newLine();
                }
            }
            Files.move(tempFile.toPath(), catalogFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Lỗi lưu file catalog: " + e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Đồng bộ hóa chỉ mục với các tệp tin vật lý trong thư mục máy chủ
     */
    public void rescanPhysicalFiles() {
        try {
            List<FileMetadata> physicalFiles = FileUtil.scanDirectory(rootDir);
            for (FileMetadata phys : physicalFiles) {
                FileMetadata existing = catalog.get(phys.getRelativePath());
                if (existing == null) {
                    phys.setVersion(1);
                    phys.setUpdatedBy("ServerInit");
                    catalog.put(phys.getRelativePath(), phys);
                } else if (!existing.getSha256().equalsIgnoreCase(phys.getSha256())) {
                    existing.setSize(phys.getSize());
                    existing.setLastModified(phys.getLastModified());
                    existing.setSha256(phys.getSha256());
                    existing.setVersion(existing.getVersion() + 1);
                }
            }
            // Loại bỏ khỏi catalog những file vật lý không còn tồn tại
            catalog.keySet().removeIf(relPath -> {
                try {
                    File target = FileUtil.resolveSafe(rootDir, relPath);
                    return !target.exists() || !target.isFile();
                } catch (Exception e) {
                    return true;
                }
            });
            persistCatalog();
        } catch (IOException e) {
            System.err.println("Lỗi quét tệp vật lý máy chủ: " + e.getMessage());
        }
    }

    /**
     * Lấy toàn bộ danh sách metadata của các tệp tin đang quản lý
     */
    public List<FileMetadata> getAllFiles() {
        lock.readLock().lock();
        try {
            List<FileMetadata> list = new ArrayList<>(catalog.values());
            Collections.sort(list);
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    public FileMetadata getMetadata(String relativePath) {
        return catalog.get(FileMetadata.normalizePath(relativePath));
    }

    /**
     * Kiểm tra khả năng phát sinh đụng độ (Conflict Detection)
     * Đụng độ xảy ra khi Client tải lên phiên bản cũ hơn phiên bản hiện tại trên Server
     * và mã băm SHA-256 khác nhau.
     */
    public boolean isConflict(FileMetadata clientMeta) {
        if (clientMeta == null) return false;
        FileMetadata serverMeta = catalog.get(clientMeta.getRelativePath());
        if (serverMeta == null) return false;

        // Nếu mã SHA-256 hoàn toàn giống nhau -> không xung đột
        if (serverMeta.getSha256().equalsIgnoreCase(clientMeta.getSha256())) {
            return false;
        }

        // Nếu server có phiên bản mới hơn phiên bản client đang có -> xung đột
        if (serverMeta.getVersion() > clientMeta.getVersion()) {
            return true;
        }

        return false;
    }

    /**
     * Lưu tệp tin mới hoặc tệp tin cập nhật được tải lên từ Client
     * Tự động sao lưu phiên bản cũ vào thư mục .versions/
     */
    public FileMetadata commitUpload(String relativePath, File tempFile, String clientName, long clientLastModified) throws IOException {
        lock.writeLock().lock();
        try {
            File targetFile = FileUtil.resolveSafe(rootDir, relativePath);
            FileUtil.ensureParentDirectory(targetFile);

            FileMetadata existing = catalog.get(FileMetadata.normalizePath(relativePath));
            int newVersion = 1;

            if (targetFile.exists() && existing != null) {
                // Đã tồn tại phiên bản trước -> Sao lưu phiên bản cũ vào .versions/
                newVersion = existing.getVersion() + 1;
                archiveVersion(relativePath, targetFile, existing.getVersion(), existing.getSha256());
            }

            // Ghi đè tệp đích bằng tệp tạm đã tải lên xong
            Files.move(tempFile.toPath(), targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            targetFile.setLastModified(clientLastModified);

            String sha256 = FileUtil.calculateSHA256(targetFile);
            FileMetadata newMeta = new FileMetadata(
                    relativePath,
                    targetFile.length(),
                    targetFile.lastModified(),
                    sha256,
                    newVersion,
                    false,
                    clientName
            );

            catalog.put(newMeta.getRelativePath(), newMeta);
            persistCatalog();
            return newMeta;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sao lưu tệp tin cũ vào kho lưu trữ phiên bản lịch sử
     */
    private void archiveVersion(String relativePath, File sourceFile, int version, String sha256) {
        try {
            File verSubDir = new File(versionsDir, FileMetadata.normalizePath(relativePath));
            if (!verSubDir.exists()) {
                verSubDir.mkdirs();
            }
            String shortSha = sha256.length() > 8 ? sha256.substring(0, 8) : sha256;
            String archiveName = String.format("v%d_%d_%s", version, System.currentTimeMillis(), shortSha);
            File archiveFile = new File(verSubDir, archiveName);
            Files.copy(sourceFile.toPath(), archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Lỗi lưu trữ phiên bản lịch sử: " + e.getMessage());
        }
    }

    /**
     * Xóa một tệp tin khỏi máy chủ
     */
    public boolean deleteFile(String relativePath, String clientName) throws IOException {
        lock.writeLock().lock();
        try {
            String norm = FileMetadata.normalizePath(relativePath);
            File target = FileUtil.resolveSafe(rootDir, norm);
            FileMetadata existing = catalog.get(norm);

            if (target.exists()) {
                if (existing != null) {
                    archiveVersion(norm, target, existing.getVersion(), existing.getSha256());
                }
                boolean deleted = target.delete();
                if (deleted) {
                    catalog.remove(norm);
                    persistCatalog();
                    return true;
                }
            } else if (existing != null) {
                catalog.remove(norm);
                persistCatalog();
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lấy danh sách các phiên bản đã lưu trong lịch sử của một tệp
     */
    public List<String> getVersionHistory(String relativePath) {
        List<String> list = new ArrayList<>();
        File verSubDir = new File(versionsDir, FileMetadata.normalizePath(relativePath));
        if (verSubDir.exists() && verSubDir.isDirectory()) {
            File[] files = verSubDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    list.add(f.getName());
                }
                Collections.sort(list);
            }
        }
        return list;
    }

    /**
     * Khôi phục tệp về một phiên bản lịch sử cụ thể
     */
    public FileMetadata rollbackVersion(String relativePath, String versionName, String operatorName) throws IOException {
        lock.writeLock().lock();
        try {
            File verFile = new File(new File(versionsDir, FileMetadata.normalizePath(relativePath)), versionName);
            if (!verFile.exists()) {
                throw new FileNotFoundException("Không tìm thấy bản lưu: " + versionName);
            }
            File targetFile = FileUtil.resolveSafe(rootDir, relativePath);
            FileUtil.ensureParentDirectory(targetFile);

            FileMetadata current = catalog.get(FileMetadata.normalizePath(relativePath));
            int nextVersion = (current != null) ? current.getVersion() + 1 : 1;

            if (targetFile.exists() && current != null) {
                archiveVersion(relativePath, targetFile, current.getVersion(), current.getSha256());
            }

            Files.copy(verFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            targetFile.setLastModified(System.currentTimeMillis());

            String sha = FileUtil.calculateSHA256(targetFile);
            FileMetadata rolledMeta = new FileMetadata(
                    relativePath,
                    targetFile.length(),
                    targetFile.lastModified(),
                    sha,
                    nextVersion,
                    false,
                    operatorName + " (Rollback)"
            );
            catalog.put(rolledMeta.getRelativePath(), rolledMeta);
            persistCatalog();
            return rolledMeta;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
