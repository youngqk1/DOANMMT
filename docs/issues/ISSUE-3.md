# Issue #3: Xây dựng Máy chủ Đa luồng & Quản lý Kho lưu trữ

- **Mã Issue:** #3
- **Người thực hiện (Assignee):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Nhãn (Labels):** `backend`, `multithreading`, `server`
- **Cột mốc (Milestone):** Tuần 2: Core Đồng bộ Đa Luồng & Quản lý Kho
- **Trạng thái:** Closed (Đã hoàn thành)

---

### 1. Mục tiêu công việc
Hiện thực máy chủ mạng đa luồng và kho quản lý tệp tin trung tâm:
- Khởi động `ServerSocket` lắng nghe kết nối đồng thời từ nhiều Client thông qua Thread Pool (`ExecutorService`).
- Xây dựng `ClientHandler` thực thi trên từng tiểu luồng độc lập, xử lý các lệnh: Handshake, List Catalog, Upload Chunking, Download Chunking, Delete.
- Xây dựng `ServerStorageManager` quản lý danh mục `catalog` bằng `ConcurrentHashMap` và khóa đọc/ghi `ReentrantReadWriteLock` bảo đảm an toàn dữ liệu.
- Lưu trữ dữ liệu tệp tin an toàn (Atomic Move) và lưu giữ tệp metadata `.catalog.meta`.
- Hiện thực cơ chế phát tán sự kiện thời gian thực (`broadcastNotification`) đến toàn bộ các Client khác đang kết nối khi kho tệp thay đổi.

### 2. Các tệp tin hiện thực
- `src/com/filesync/server/SyncServer.java`
- `src/com/filesync/server/ClientHandler.java`
- `src/com/filesync/server/ServerStorageManager.java`

### 3. Tiêu chí nghiệm thu (Acceptance Criteria)
- [x] Máy chủ tiếp nhận đồng thời nhiều Client cùng kết nối mà không bị treo/nghẽn.
- [x] Tệp tải lên được lưu trữ toàn vẹn, kiểm tra SHA-256 chính xác.
- [x] Broadcast thông báo đến tất cả các Client khác khi có một Client tải tệp mới.
