# Merge Request !3: Multi-threaded Sync Server & Storage Manager

- **Nguồn (Source Branch):** `feature/2280601477-issue-3-server-storage-manager`
- **Đích (Target Branch):** `main`
- **Người thực hiện (Author):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Hình thức thực hiện:** Đồ án cá nhân (Individual Project)
- **Trạng thái:** MERGED
- **Liên kết Issue:** Closes #3

---

### 1. Nội dung thay đổi (Description)
- Hiện thực máy chủ đa luồng `SyncServer` sử dụng `ServerSocket` và Thread Pool `Executors.newCachedThreadPool()`.
- Xây dựng `ClientHandler` xử lý từng phiên kết nối Client: bóc tách lệnh, nhận/gửi khối tệp tin 64KB, kiểm tra SHA-256 đối chiếu toàn vẹn.
- Xây dựng `ServerStorageManager` quản lý kho lưu trữ tệp, lưu vết metadata trong `.catalog.meta` và sao lưu phiên bản cũ.
- Hiện thực phương thức `broadcastNotification` phát tán tín hiệu thông báo thời gian thực đến toàn bộ các kênh NOTIFY của các Client khác khi có tệp thay đổi.

### 2. Đánh giá ảnh hưởng (Impact Assessment)
- Hoàn tất phần lõi của Máy chủ Backend. Máy chủ sẵn sàng kết nối và tiếp nhận dữ liệu từ các máy khách.

### 3. Kết quả thử nghiệm và Bằng chứng thực thi
- Thử nghiệm kết nối đồng thời từ 2 Client gửi nhận dữ liệu song song qua socket 127.0.0.1:9999.
- Dữ liệu tệp tải lên được lưu trữ nguyên vẹn và cập nhật danh mục máy chủ chính xác.

---

### 4. Biên bản Thẩm định & Nghiệm thu Kỹ thuật (Verification Checklist)
- **Người thẩm định:** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Checklist nghiệm thu:**
  - [x] Máy chủ xử lý đa luồng tốt, sử dụng ConcurrentHashMap và ReentrantReadWriteLock đảm bảo an toàn truy cập dữ liệu đồng thời.
  - [x] Phân tách rõ ràng 2 kênh Command và Notify, loại bỏ nguy cơ tắc nghẽn hoặc xung đột luồng mạng.
  - [x] Lưu trữ tệp tin an toàn theo cơ chế Atomic Move, sao lưu phiên bản cũ vào thư mục `.versions/`.
- **Quyết định (Status):** **VERIFIED & APPROVED FOR MERGE**
