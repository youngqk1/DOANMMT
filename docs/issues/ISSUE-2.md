# Issue #2: Hiện thực Động cơ Đồng bộ Tệp tin & WatchService

- **Mã Issue:** #2
- **Người thực hiện (Assignee):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Nhãn (Labels):** `backend`, `enhancement`, `sync-engine`
- **Cột mốc (Milestone):** Tuần 1: Kiến trúc Hệ thống & Giao thức Mạng
- **Trạng thái:** Closed (Đã hoàn thành)

---

### 1. Mục tiêu công việc
Xây dựng động cơ đồng bộ tệp tin cốt lõi phía Client:
- Tích hợp `java.nio.file.WatchService` để lắng nghe tự động mọi thao tác tạo mới (CREATE), chỉnh sửa (MODIFY) và xóa (DELETE) trên thư mục cục bộ.
- Xây dựng cơ chế Debouncing nhằm gộp các sự kiện liên tiếp sinh ra bởi hệ điều hành hoặc trình soạn thảo khi lưu tệp, loại bỏ các sự kiện trùng lặp vô nghĩa.
- Cơ chế chặn phản xạ vòng lặp (Suppression): Bỏ qua giám sát tạm thời khi Client đang tải tệp từ Server về máy để tránh tình trạng tải về rồi lại kích hoạt tải lên ngược lại.
- Xây dựng lớp `SyncClient` quản lý kết nối socket điều khiển và gửi/nhận dữ liệu khối.

### 2. Các tệp tin hiện thực
- `src/com/filesync/client/DirectoryWatcher.java`
- `src/com/filesync/client/SyncClient.java`

### 3. Tiêu chí nghiệm thu (Acceptance Criteria)
- [x] Tự động phát hiện khi tệp được thêm mới vào thư mục giám sát.
- [x] Debouncing thành công trong khoảng trễ 600ms không bị flood sự kiện.
- [x] Không bị lặp vô tận khi nhận tệp tải về từ Server.
