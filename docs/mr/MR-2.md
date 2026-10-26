# Merge Request !2: File Synchronization Engine & WatchService

- **Nguồn (Source Branch):** `feature/2280601477-issue-2-file-sync-engine`
- **Đích (Target Branch):** `main`
- **Người thực hiện (Author):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Hình thức thực hiện:** Đồ án cá nhân (Individual Project)
- **Trạng thái:** MERGED
- **Liên kết Issue:** Closes #2

---

### 1. Nội dung thay đổi (Description)
- Xây dựng lớp `DirectoryWatcher` sử dụng `WatchService` để lắng nghe các sự kiện CREATE, MODIFY, DELETE trên thư mục cục bộ.
- Bổ sung cơ chế Debouncing kết hợp `ScheduledExecutorService` (độ trễ 600ms) để loại bỏ các sự kiện trùng lặp.
- Thiết lập cơ chế `suppress(path)` / `unsuppress(path)` tránh tạo vòng lặp vô tận khi Client tải tệp từ máy chủ về.
- Xây dựng tầng mạng `SyncClient` hỗ trợ tải lên và tải về tệp tin theo khối 64KB kèm callback báo cáo tiến độ.

### 2. Đánh giá ảnh hưởng (Impact Assessment)
- Hoàn thiện tầng giao tiếp của Client, chuẩn bị cho việc tích hợp bộ điều phối SyncEngine và giao diện đồ họa.

### 3. Kết quả thử nghiệm và Bằng chứng thực thi
- Đã kiểm tra WatchService với các thao tác tạo/sửa/xóa file thủ công qua Windows Explorer.
- Sự kiện được bắt chính xác và gọi hàm callback tương ứng sau đúng 600ms debounce.

---

### 4. Biên bản Thẩm định & Nghiệm thu Kỹ thuật (Verification Checklist)
- **Người thẩm định:** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Checklist nghiệm thu:**
  - [x] Cơ chế debouncing 600ms loại bỏ triệt để hiện tượng rung sự kiện (event flapping) của Windows.
  - [x] Cơ chế suppressedPaths hoạt động hiệu quả, triệt tiêu hoàn toàn vòng lặp phản xạ vô tận (Echo Loop).
  - [x] Luồng Thread của WatchService được đặt cờ daemon hợp lý, không làm treo JVM khi đóng ứng dụng.
- **Quyết định (Status):** **VERIFIED & APPROVED FOR MERGE**
