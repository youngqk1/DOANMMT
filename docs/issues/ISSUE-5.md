# Issue #5: Xử lý Đụng độ phiên bản (Conflict Resolution) và Lịch sử Tệp tin

- **Mã Issue:** #5
- **Người thực hiện (Assignee):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Nhãn (Labels):** `backend`, `conflict-resolution`, `versioning`
- **Cột mốc (Milestone):** Tuần 3: Đụng độ Phiên Bản, GUI Monitor & Kiểm Thử
- **Trạng thái:** Closed (Đã hoàn thành)

---

### 1. Mục tiêu công việc
Hiện thực giải thuật phát hiện đụng độ sửa đổi tệp đồng thời và cơ chế phân giải đụng độ chuẩn bảo toàn dữ liệu:
- Cơ chế kiểm soát phiên bản lạc quan (Optimistic Concurrency Control): So sánh số phiên bản mà Client đang nắm giữ với số phiên bản hiện tại trên Server.
- Nếu Client cố gắng tải lên một phiên bản dựa trên bản gốc cũ mà Server đã nhận bản mới từ Client khác với SHA-256 khác -> Server cảnh báo lệnh `CONFLICT_DETECTED`.
- Cơ chế giải quyết đụng độ (Conflict Resolution Policy): Không ghi đè làm mất mát dữ liệu của người dùng. Thay vào đó, tệp cục bộ được tự động đổi tên thành `[tên_tệp]_conflict_[client]_[thời_gian].[đuôi]`, sau đó Client tải bản chuẩn từ Server về để cập nhật tệp gốc, và đẩy tệp conflict lên Server để các thành viên khác cùng kiểm tra nội dung khác biệt.
- Xây dựng cơ chế lưu trữ lịch sử đa phiên bản (File Versioning) trong thư mục `.versions/` trên Server và hỗ trợ khôi phục (Rollback).

### 2. Các tệp tin hiện thực
- Cập nhật `src/com/filesync/client/SyncEngine.java` (Phương thức `handleConflict`, OCC version check)
- Cập nhật `src/com/filesync/server/ServerStorageManager.java` (Phương thức `archiveVersion`, `getVersionHistory`, `rollbackVersion`)
- Cập nhật `src/com/filesync/server/ClientHandler.java` (Xử lý `GET_VERSIONS_REQ`, `ROLLBACK_REQ`)

### 3. Tiêu chí nghiệm thu (Acceptance Criteria)
- [x] Phát hiện chính xác đụng độ khi 2 Client sửa cùng 1 tệp tin trong trạng thái chưa đồng bộ.
- [x] Tuyệt đối không làm mất dữ liệu của bất kỳ Client nào.
- [x] Phiên bản cũ được sao lưu đầy đủ vào thư mục `.versions/`.
