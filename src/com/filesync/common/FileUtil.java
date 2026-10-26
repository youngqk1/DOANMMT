package com.filesync.common;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Lớp tiện ích xử lý tệp tin, tính toán hàm băm SHA-256 và bảo mật đường dẫn
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public final class FileUtil {
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private FileUtil() {}

    /**
     * Tính toán mã băm SHA-256 cho một tệp tin
     */
    public static String calculateSHA256(File file) throws IOException {
        if (!file.exists() || !file.isFile()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
                byte[] buffer = new byte[Constants.BUFFER_SIZE];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Thuật toán SHA-256 không khả dụng trên JVM", e);
        }
    }

    /**
     * Tính toán mã băm SHA-256 cho mảng byte
     */
    public static String calculateSHA256(byte[] data) {
        if (data == null || data.length == 0) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Lỗi thuật toán SHA-256", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Định dạng kích thước tệp hiển thị thân thiện với người dùng (Bytes, KB, MB, GB)
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format(Locale.US, "%.2f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    /**
     * Định dạng mốc thời gian epoch millis sang chuỗi ngày giờ
     */
    public static String formatTimestamp(long millis) {
        return new SimpleDateFormat(DATE_FORMAT_PATTERN).format(new Date(millis));
    }

    /**
     * Quét toàn bộ thư mục và tạo danh sách FileMetadata cho từng tệp
     */
    public static List<FileMetadata> scanDirectory(File baseDir) throws IOException {
        List<FileMetadata> list = new ArrayList<>();
        if (!baseDir.exists() || !baseDir.isDirectory()) return list;

        Path basePath = baseDir.toPath().toRealPath();
        Files.walk(basePath)
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        String rel = basePath.relativize(p).toString().replace('\\', '/');
                        // Bỏ qua các tệp ẩn hệ thống và tệp metadata nội bộ
                        if (rel.startsWith(".") || rel.contains("/.") || rel.endsWith("~") || rel.endsWith(".tmp")) {
                            return;
                        }
                        File file = p.toFile();
                        long size = file.length();
                        long lastMod = file.lastModified();
                        String sha = calculateSHA256(file);
                        list.add(new FileMetadata(rel, size, lastMod, sha));
                    } catch (Exception ignored) {
                    }
                });

        Collections.sort(list);
        return list;
    }

    /**
     * Kiểm tra chống tấn công Path Traversal (ngăn chặn đường dẫn chứa .. thoát khỏi thư mục gốc)
     */
    public static File resolveSafe(File baseDir, String relativePath) throws IOException {
        String safeRel = FileMetadata.normalizePath(relativePath);
        File target = new File(baseDir, safeRel).getCanonicalFile();
        File baseCanonical = baseDir.getCanonicalFile();
        if (!target.getPath().startsWith(baseCanonical.getPath())) {
            throw new SecurityException("Phát hiện nguy cơ Path Traversal: " + relativePath);
        }
        return target;
    }

    /**
     * Đảm bảo thư mục cha tồn tại
     */
    public static void ensureParentDirectory(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    /**
     * Ghi dữ liệu an toàn kiểu Atomic (ghi vào tệp tạm trước rồi đổi tên)
     */
    public static void atomicWrite(File targetFile, byte[] data) throws IOException {
        ensureParentDirectory(targetFile);
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".atomic.tmp");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
            fos.flush();
        }
        Files.move(tempFile.toPath(), targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Xóa thư mục đệ quy (hỗ trợ kiểm thử và dọn dẹp)
     */
    public static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
