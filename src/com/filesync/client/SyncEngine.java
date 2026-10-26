package com.filesync.client;

import com.filesync.common.*;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Động cơ điều phối đồng bộ tệp tin hai chiều (Bidirectional Sync Engine)
 * Xử lý đối chiếu danh mục, xung đột phiên bản và đồng bộ hóa thời gian thực
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class SyncEngine {
    public enum SyncStatus { SYNCED, UPLOADING, DOWNLOADING, CONFLICT, LOCAL_NEW, SERVER_NEW, ERROR }

    private final File localDir;
    private final SyncClient client;
    private final DirectoryWatcher watcher;
    private final Map<String, FileMetadata> knownLocalFiles = new ConcurrentHashMap<>();
    private final ReentrantLock syncLock = new ReentrantLock();

    private volatile boolean autoSyncEnabled = true;
    private Consumer<String> logListener;
    private BiConsumer<String, SyncStatus> fileStatusListener;
    private BiConsumer<Long, Long> progressListener;
    private Runnable catalogChangeListener;

    public SyncEngine(File localDirectory, SyncClient client) {
        this.localDir = localDirectory;
        this.client = client;

        if (!localDir.exists()) {
            localDir.mkdirs();
        }

        this.watcher = new DirectoryWatcher(localDir, this::onLocalFileEvent);
        this.client.setChangeNotificationListener(this::onServerNotification);
    }

    public File getLocalDir() { return localDir; }
    public SyncClient getClient() { return client; }
    public boolean isAutoSyncEnabled() { return autoSyncEnabled; }

    public void setAutoSyncEnabled(boolean enabled) { this.autoSyncEnabled = enabled; }
    public void setLogListener(Consumer<String> listener) { this.logListener = listener; }
    public void setFileStatusListener(BiConsumer<String, SyncStatus> listener) { this.fileStatusListener = listener; }
    public void setProgressListener(BiConsumer<Long, Long> listener) { this.progressListener = listener; }
    public void setCatalogChangeListener(Runnable listener) { this.catalogChangeListener = listener; }

    public void start() throws IOException {
        watcher.start();
        log("Đã khởi động giám sát thư mục cục bộ: " + localDir.getAbsolutePath());
    }

    public void stop() {
        watcher.stop();
        log("Đã dừng giám sát thư mục cục bộ.");
    }

    /**
     * Thực thi đồng bộ hóa toàn diện 2 chiều giữa Client và Server (Full Sync)
     */
    public boolean syncAll() {
        if (!client.isConnected()) {
            log("Chưa kết nối máy chủ, không thể đồng bộ.");
            return false;
        }

        syncLock.lock();
        try {
            log("=== BẮT ĐẦU ĐỒNG BỘ TOÀN DIỆN 2 CHIỀU ===");

            // 1. Quét tệp tin cục bộ hiện tại
            List<FileMetadata> localList = FileUtil.scanDirectory(localDir);
            Map<String, FileMetadata> localMap = new HashMap<>();
            for (FileMetadata l : localList) {
                localMap.put(l.getRelativePath(), l);
            }

            // 2. Lấy danh mục tệp tin từ Server
            List<FileMetadata> serverList = client.fetchServerCatalog();
            Map<String, FileMetadata> serverMap = new HashMap<>();
            for (FileMetadata s : serverList) {
                serverMap.put(s.getRelativePath(), s);
            }

            // 3. Xử lý các tệp có ở Client
            for (FileMetadata localMeta : localList) {
                String relPath = localMeta.getRelativePath();
                File localFile = new File(localDir, relPath);
                FileMetadata serverMeta = serverMap.get(relPath);

                if (serverMeta == null) {
                    // Tệp chỉ có ở Local -> Tải lên Server
                    log(String.format("Phát hiện tệp mới cục bộ [%s], tiến hành tải lên Server...", relPath));
                    updateStatus(relPath, SyncStatus.UPLOADING);
                    boolean ok = client.uploadFile(localFile, relPath, 1, progressListener);
                    updateStatus(relPath, ok ? SyncStatus.SYNCED : SyncStatus.ERROR);
                } else {
                    // Tệp tồn tại ở cả 2 bên -> Kiểm tra SHA-256
                    if (localMeta.getSha256().equalsIgnoreCase(serverMeta.getSha256())) {
                        updateStatus(relPath, SyncStatus.SYNCED);
                    } else {
                        // Kiểm tra phiên bản cục bộ đang giữ để phát hiện xung đột Optimistic Concurrency
                        FileMetadata known = knownLocalFiles.get(relPath);
                        int localBaseVersion = (known != null) ? known.getVersion() : 1;

                        if (localBaseVersion < serverMeta.getVersion()) {
                            // Máy chủ đã có phiên bản cao hơn -> Xung đột phiên bản!
                            handleConflict(localMeta, serverMeta);
                        } else if (localMeta.getLastModified() > serverMeta.getLastModified() + 1000) {
                            // Bản cục bộ mới hơn -> Đẩy lên Server
                            log(String.format("Tệp cục bộ [%s] có thay đổi mới hơn Server, tải lên bản mới...", relPath));
                            updateStatus(relPath, SyncStatus.UPLOADING);
                            boolean ok = client.uploadFile(localFile, relPath, localBaseVersion, progressListener);
                            if (!ok) {
                                handleConflict(localMeta, serverMeta);
                            } else {
                                updateStatus(relPath, SyncStatus.SYNCED);
                                knownLocalFiles.put(relPath, new FileMetadata(relPath, localFile.length(), localFile.lastModified(), localMeta.getSha256(), localBaseVersion + 1, false, client.getClientId()));
                            }
                        } else if (serverMeta.getLastModified() > localMeta.getLastModified() + 1000) {
                            // Bản trên Server mới hơn -> Tải về máy
                            log(String.format("Tệp trên Server [%s] mới hơn bản cục bộ, tiến hành tải về...", relPath));
                            updateStatus(relPath, SyncStatus.DOWNLOADING);
                            watcher.suppress(relPath);
                            try {
                                boolean ok = client.downloadFile(relPath, localFile, progressListener);
                                if (ok) {
                                    updateStatus(relPath, SyncStatus.SYNCED);
                                    knownLocalFiles.put(relPath, serverMeta);
                                } else {
                                    updateStatus(relPath, SyncStatus.ERROR);
                                }
                            } finally {
                                watcher.unsuppress(relPath);
                            }
                        } else {
                            // Cùng sửa đổi trong khoảng thời gian sát sao -> Xử lý đụng độ
                            handleConflict(localMeta, serverMeta);
                        }
                    }
                }
            }

            // 4. Xử lý các tệp có trên Server nhưng chưa có ở Client -> Tải về
            for (FileMetadata serverMeta : serverList) {
                String relPath = serverMeta.getRelativePath();
                if (!localMap.containsKey(relPath)) {
                    log(String.format("Tệp mới từ Server [%s], tiến hành tải về Client...", relPath));
                    File target = new File(localDir, relPath);
                    updateStatus(relPath, SyncStatus.DOWNLOADING);
                    watcher.suppress(relPath);
                    try {
                        boolean ok = client.downloadFile(relPath, target, progressListener);
                        updateStatus(relPath, ok ? SyncStatus.SYNCED : SyncStatus.ERROR);
                    } finally {
                        watcher.unsuppress(relPath);
                    }
                }
            }

            // Làm mới bộ nhớ tệp tin đã biết
            refreshKnownFiles();
            log("=== HOÀN TẤT ĐỒNG BỘ TOÀN DIỆN ===");
            if (catalogChangeListener != null) {
                catalogChangeListener.run();
            }
            return true;
        } catch (Exception e) {
            log("Lỗi trong quá trình đồng bộ: " + e.getMessage());
            return false;
        } finally {
            syncLock.unlock();
        }
    }

    /**
     * Xử lý xung đột phiên bản (Conflict Resolution) theo chuẩn an toàn không mất dữ liệu:
     * Đổi tên bản cục bộ thành [tên]_conflict_[client]_[thời_gian].[đuôi], sau đó tải bản gốc từ Server về
     */
    private void handleConflict(FileMetadata localMeta, FileMetadata serverMeta) {
        String relPath = localMeta.getRelativePath();
        log(String.format("[CONFLICT] Xung đột tệp [%s]! Tiến hành tách nhánh bảo toàn dữ liệu...", relPath));
        updateStatus(relPath, SyncStatus.CONFLICT);

        try {
            File currentLocal = new File(localDir, relPath);
            String ext = "";
            String base = relPath;
            int dotIdx = relPath.lastIndexOf('.');
            if (dotIdx > 0) {
                base = relPath.substring(0, dotIdx);
                ext = relPath.substring(dotIdx);
            }

            String conflictRelPath = String.format("%s%s%s_%d%s",
                    base, Constants.CONFLICT_PREFIX, client.getClientId(), System.currentTimeMillis() / 1000, ext);
            File conflictFile = new File(localDir, conflictRelPath);

            // Đổi tên tệp cục bộ thành tệp conflict
            currentLocal.renameTo(conflictFile);
            log("Đã lưu bản cục bộ thành: " + conflictRelPath);

            // Tải bản chuẩn từ Server về cho tệp gốc
            watcher.suppress(relPath);
            try {
                client.downloadFile(relPath, currentLocal, progressListener);
                updateStatus(relPath, SyncStatus.SYNCED);
            } finally {
                watcher.unsuppress(relPath);
            }

            // Đồng thời tải tệp conflict lên Server để các thành viên khác cùng thấy phiên bản sửa đổi này
            client.uploadFile(conflictFile, conflictRelPath, 1, progressListener);
            updateStatus(conflictRelPath, SyncStatus.SYNCED);
        } catch (Exception e) {
            log("Lỗi giải quyết xung đột: " + e.getMessage());
        }
    }

    /**
     * Xử lý sự kiện thời gian thực khi người dùng tạo/sửa/xóa tệp trong thư mục giám sát
     */
    private void onLocalFileEvent(DirectoryWatcher.EventType type, String relativePath) {
        if (!autoSyncEnabled || !client.isConnected()) return;

        syncLock.lock();
        try {
            File localFile = new File(localDir, relativePath);
            switch (type) {
                case CREATE:
                case MODIFY:
                    if (localFile.exists() && localFile.isFile()) {
                        String sha = FileUtil.calculateSHA256(localFile);
                        FileMetadata known = knownLocalFiles.get(relativePath);
                        if (known != null && known.getSha256().equalsIgnoreCase(sha)) {
                            return; // Dữ liệu không thay đổi
                        }

                        log(String.format("[LOCAL CHANGE] Phát hiện %s trên tệp [%s]", type, relativePath));
                        updateStatus(relativePath, SyncStatus.UPLOADING);
                        boolean ok = client.uploadFile(localFile, relativePath, 1, progressListener);
                        if (ok) {
                            updateStatus(relativePath, SyncStatus.SYNCED);
                            knownLocalFiles.put(relativePath, new FileMetadata(relativePath, localFile.length(), localFile.lastModified(), sha));
                        } else {
                            updateStatus(relativePath, SyncStatus.ERROR);
                        }
                    }
                    break;

                case DELETE:
                    log(String.format("[LOCAL DELETE] Người dùng đã xóa tệp [%s]", relativePath));
                    client.deleteFileOnServer(relativePath);
                    knownLocalFiles.remove(relativePath);
                    break;
            }
            if (catalogChangeListener != null) {
                catalogChangeListener.run();
            }
        } catch (Exception e) {
            log("Lỗi xử lý sự kiện tệp cục bộ: " + e.getMessage());
        } finally {
            syncLock.unlock();
        }
    }

    /**
     * Tiếp nhận thông báo đẩy từ Server khi có client khác thay đổi tệp (Real-time Broadcast)
     */
    private void onServerNotification(FileMetadata serverMeta) {
        if (!autoSyncEnabled) return;

        syncLock.lock();
        try {
            String relPath = serverMeta.getRelativePath();
            File localFile = new File(localDir, relPath);

            if (serverMeta.isDeleted()) {
                log(String.format("[SERVER NOTIFY] Tệp [%s] đã bị xóa từ người dùng [%s]", relPath, serverMeta.getUpdatedBy()));
                if (localFile.exists()) {
                    watcher.suppress(relPath);
                    try {
                        localFile.delete();
                        knownLocalFiles.remove(relPath);
                    } finally {
                        watcher.unsuppress(relPath);
                    }
                }
            } else {
                log(String.format("[SERVER NOTIFY] Nhận thông báo tệp [%s] được cập nhật bởi [%s]", relPath, serverMeta.getUpdatedBy()));

                // Kiểm tra xem máy cục bộ có thay đổi chưa commit không
                if (localFile.exists()) {
                    String localSha = FileUtil.calculateSHA256(localFile);
                    if (localSha.equalsIgnoreCase(serverMeta.getSha256())) {
                        return; // Đã đồng bộ rồi
                    }
                }

                // Tải bản mới từ Server về
                updateStatus(relPath, SyncStatus.DOWNLOADING);
                watcher.suppress(relPath);
                try {
                    boolean ok = client.downloadFile(relPath, localFile, progressListener);
                    if (ok) {
                        updateStatus(relPath, SyncStatus.SYNCED);
                        refreshKnownFiles();
                    } else {
                        updateStatus(relPath, SyncStatus.ERROR);
                    }
                } finally {
                    watcher.unsuppress(relPath);
                }
            }

            if (catalogChangeListener != null) {
                catalogChangeListener.run();
            }
        } catch (Exception e) {
            log("Lỗi xử lý thông báo Server: " + e.getMessage());
        } finally {
            syncLock.unlock();
        }
    }

    private void refreshKnownFiles() {
        try {
            List<FileMetadata> list = FileUtil.scanDirectory(localDir);
            for (FileMetadata m : list) {
                FileMetadata existing = knownLocalFiles.get(m.getRelativePath());
                if (existing != null) {
                    m.setVersion(existing.getVersion());
                }
                knownLocalFiles.put(m.getRelativePath(), m);
            }
            // Loại bỏ các tệp không còn tồn tại
            knownLocalFiles.keySet().removeIf(p -> !new File(localDir, p).exists());
        } catch (IOException ignored) {}
    }

    private void updateStatus(String path, SyncStatus status) {
        if (fileStatusListener != null) {
            fileStatusListener.accept(path, status);
        }
    }

    private void log(String msg) {
        String logEntry = String.format("[%s] %s", FileUtil.formatTimestamp(System.currentTimeMillis()), msg);
        System.out.println(logEntry);
        if (logListener != null) {
            logListener.accept(logEntry);
        }
    }
}
