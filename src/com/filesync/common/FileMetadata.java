package com.filesync.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Lớp chứa siêu dữ liệu (Metadata) mô tả thông tin chi tiết của một tệp tin
 * Được truyền tải qua mạng để đồng bộ và đối chiếu trạng thái giữa Client và Server
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class FileMetadata implements Serializable, Comparable<FileMetadata> {
    private static final long serialVersionUID = 1L;

    private String relativePath;     // Đường dẫn tương đối (ví dụ: "notes/todo.txt")
    private long size;               // Kích thước tệp tin (bytes)
    private long lastModified;       // Thời điểm cập nhật cuối cùng (epoch millis)
    private String sha256;           // Chuỗi băm SHA-256 đối chiếu toàn vẹn dữ liệu
    private int version;             // Số thứ tự phiên bản (tăng dần 1, 2, 3...)
    private boolean isDeleted;       // Cờ đánh dấu tệp đã bị xóa (Tombstone)
    private String updatedBy;        // Tên client hoặc định danh tài khoản cập nhật

    public FileMetadata() {
        this.relativePath = "";
        this.size = 0L;
        this.lastModified = System.currentTimeMillis();
        this.sha256 = "";
        this.version = 1;
        this.isDeleted = false;
        this.updatedBy = "Unknown";
    }

    public FileMetadata(String relativePath, long size, long lastModified, String sha256) {
        this(relativePath, size, lastModified, sha256, 1, false, "Client");
    }

    public FileMetadata(String relativePath, long size, long lastModified, String sha256, int version, boolean isDeleted, String updatedBy) {
        this.relativePath = normalizePath(relativePath);
        this.size = size;
        this.lastModified = lastModified;
        this.sha256 = sha256 != null ? sha256 : "";
        this.version = version;
        this.isDeleted = isDeleted;
        this.updatedBy = updatedBy != null ? updatedBy : "Client";
    }

    public static String normalizePath(String path) {
        if (path == null) return "";
        return path.replace('\\', '/').replaceAll("^/+", "");
    }

    // Getters và Setters
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = normalizePath(relativePath); }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    public String getFileName() {
        int idx = relativePath.lastIndexOf('/');
        return idx >= 0 ? relativePath.substring(idx + 1) : relativePath;
    }

    /**
     * Chuyển đổi đối tượng sang chuỗi mã hóa đơn giản để truyền qua mạng không phụ thuộc thư viện ngoài
     */
    public String encode() {
        return escape(relativePath) + "\t" +
               size + "\t" +
               lastModified + "\t" +
               escape(sha256) + "\t" +
               version + "\t" +
               isDeleted + "\t" +
               escape(updatedBy);
    }

    /**
     * Giải mã chuỗi thành đối tượng FileMetadata
     */
    public static FileMetadata decode(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String[] parts = raw.split("\t", -1);
        if (parts.length < 4) return null;
        try {
            String path = unescape(parts[0]);
            long size = Long.parseLong(parts[1]);
            long lastMod = Long.parseLong(parts[2]);
            String sha = unescape(parts[3]);
            int ver = parts.length > 4 ? Integer.parseInt(parts[4]) : 1;
            boolean del = parts.length > 5 && Boolean.parseBoolean(parts[5]);
            String by = parts.length > 6 ? unescape(parts[6]) : "Client";
            return new FileMetadata(path, size, lastMod, sha, ver, del, by);
        } catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        if (s == null) return "";
        return s.replace("\\n", "\n").replace("\\t", "\t").replace("\\\\", "\\");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileMetadata)) return false;
        FileMetadata that = (FileMetadata) o;
        return Objects.equals(relativePath, that.relativePath) &&
               Objects.equals(sha256, that.sha256);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath, sha256);
    }

    @Override
    public int compareTo(FileMetadata o) {
        if (o == null) return 1;
        return this.relativePath.compareTo(o.relativePath);
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "path='" + relativePath + '\'' +
                ", size=" + size +
                ", v=" + version +
                ", sha256='" + (sha256.length() > 8 ? sha256.substring(0, 8) + "..." : sha256) + '\'' +
                ", del=" + isDeleted +
                '}';
    }
}
