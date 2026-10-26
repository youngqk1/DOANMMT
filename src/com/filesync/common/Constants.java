package com.filesync.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Các hằng số hệ thống cấu hình chung cho ứng dụng Đồng bộ Tệp tin
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public final class Constants {
    private Constants() {}

    // Cổng mạng mặc định cho Server Socket
    public static final int DEFAULT_PORT = 9999;
    public static final String DEFAULT_HOST = "127.0.0.1";

    // Kích thước khối truyền dữ liệu tệp nhị phân (64 KB)
    public static final int CHUNK_SIZE = 64 * 1024;

    // Kích thước bộ đệm đọc ghi I/O
    public static final int BUFFER_SIZE = 8192;

    // Bảng mã ký tự chuẩn toàn hệ thống
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    // Thời gian chờ trễ (debounce) cho WatchService (milliseconds)
    public static final long WATCH_DEBOUNCE_MS = 600;

    // Thời gian timeout socket (milliseconds)
    public static final int SOCKET_TIMEOUT_MS = 30000;

    // Thư mục lưu trữ phiên bản tệp tin trên máy chủ
    public static final String VERSIONS_FOLDER = ".versions";

    // Tên tệp chỉ mục danh mục lưu trữ
    public static final String CATALOG_FILE = ".catalog.meta";

    // Tiền tố nhận diện tệp đụng độ
    public static final String CONFLICT_PREFIX = "_conflict_";
}
