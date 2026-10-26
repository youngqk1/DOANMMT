package com.filesync.common;

/**
 * Danh sách các loại lệnh giao thức điều khiển giữa Client và Server
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public enum CommandType {
    // 1. Khởi tạo phiên kết nối
    HANDSHAKE,          // Client gửi định danh người dùng và tên máy
    HANDSHAKE_ACK,      // Server chấp nhận kết nối và trả về thông tin cấu hình

    // 2. Tra cứu danh mục tệp tin (Catalog)
    LIST_FILES_REQ,     // Client yêu cầu lấy danh sách tệp tin trên máy chủ
    LIST_FILES_RESP,    // Server trả về danh sách FileMetadata

    // 3. Tải tệp lên máy chủ (Upload)
    UPLOAD_REQ,         // Client gửi yêu cầu tải tệp (kèm metadata, SHA-256)
    UPLOAD_ACK,         // Server chấp thuận, sẵn sàng tiếp nhận các chunk
    UPLOAD_CHUNK,       // Gói dữ liệu nhị phân chứa 1 khối tệp
    UPLOAD_COMPLETE,    // Client thông báo kết thúc truyền toàn bộ khối tệp

    // 4. Tải tệp từ máy chủ về (Download)
    DOWNLOAD_REQ,       // Client yêu cầu tải một tệp cụ thể
    DOWNLOAD_ACK,       // Server phản hồi metadata tệp và số lượng chunk
    DOWNLOAD_CHUNK,     // Server gửi khối dữ liệu nhị phân
    DOWNLOAD_COMPLETE,  // Server thông báo kết thúc truyền tệp

    // 5. Xóa tệp tin (Delete)
    DELETE_REQ,         // Client yêu cầu xóa tệp
    DELETE_RESP,        // Phản hồi kết quả xóa tệp

    // 6. Quản lý phiên bản và lịch sử (Versioning)
    GET_VERSIONS_REQ,   // Yêu cầu danh sách các phiên bản cũ của một tệp
    GET_VERSIONS_RESP,  // Trả về danh sách phiên bản lịch sử
    ROLLBACK_REQ,       // Yêu cầu khôi phục tệp về một phiên bản trước đó
    ROLLBACK_RESP,      // Phản hồi kết quả khôi phục

    // 7. Đồng bộ theo thời gian thực (Push notification)
    NOTIFY_CHANGE,      // Server phát thông báo cho các Client khác khi có tệp thay đổi

    // 8. Đụng độ và phản hồi trạng thái
    CONFLICT_DETECTED,  // Cảnh báo phát hiện đụng độ sửa đổi tệp đồng thời
    SUCCESS,            // Thông báo thao tác thành công
    ERROR               // Thông báo lỗi xử lý
}
