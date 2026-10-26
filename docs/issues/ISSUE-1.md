# Issue #1: Xây dựng Core Network Protocol & Base Socket Framework

- **Mã Issue:** #1
- **Người thực hiện (Assignee):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Nhãn (Labels):** `core-network`, `enhancement`, `backend`
- **Cột mốc (Milestone):** Tuần 1: Kiến trúc Hệ thống & Giao thức Mạng
- **Trạng thái:** Closed (Đã hoàn thành)

---

### 1. Mục tiêu công việc
Xây dựng nền tảng giao thức mạng TCP tầng ứng dụng và cấu trúc gói tin điều khiển cho hệ thống đồng bộ tệp tin Client-Server:
- Thiết lập định dạng khung bản tin nhị phân / chuỗi UTF-8 (Framed Network Stream).
- Khởi tạo bảng mã lệnh `CommandType` bao hàm đầy đủ các hành vi: Handshake, List, Upload, Download, Delete, Notify, Conflict, Success, Error.
- Xây dựng lớp đối tượng siêu dữ liệu `FileMetadata` mô tả tệp tin (đường dẫn tương đối, kích thước, mốc thời gian sửa đổi, mã băm SHA-256, phiên bản vX, trạng thái xóa).
- Cung cấp lớp tiện ích `FileUtil` để tính toán mã băm SHA-256, chuẩn hóa đường dẫn an toàn chống tấn công Path Traversal, định dạng kích thước hiển thị.

### 2. Các tệp tin hiện thực
- `src/com/filesync/common/Constants.java`
- `src/com/filesync/common/CommandType.java`
- `src/com/filesync/common/FileMetadata.java`
- `src/com/filesync/common/FileUtil.java`
- `src/com/filesync/common/Message.java`

### 3. Tiêu chí nghiệm thu (Acceptance Criteria)
- [x] Đóng gói và giải mã khung thông điệp chính xác qua Socket InputStream/OutputStream.
- [x] Tính toán mã SHA-256 nhất quán cho cả mảng byte và tệp tin trên đĩa.
- [x] Ngăn chặn tuyệt đối các đường dẫn độc hại chứa `..` thoát khỏi thư mục gốc (Path Traversal).
- [x] Biên dịch không có lỗi cú pháp.
