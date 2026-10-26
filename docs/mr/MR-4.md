# Merge Request !4: Client GUI Dashboard & Sync Controller

- **Nguồn (Source Branch):** `feature/2280601477-issue-4-client-gui-dashboard`
- **Đích (Target Branch):** `main`
- **Người thực hiện (Author):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Hình thức thực hiện:** Đồ án cá nhân (Individual Project)
- **Trạng thái:** MERGED
- **Liên kết Issue:** Closes #4

---

### 1. Nội dung thay đổi (Description)
- Thiết kế giao diện Client đồ họa `ClientGUI` với Java Swing hiện đại:
  + Thanh kết nối máy chủ, cấu hình cổng và chọn thư mục làm việc.
  + Bảng thống kê tệp tin với TableCellRenderer tô màu trạng thái đồng bộ: Đã đồng bộ, Đang tải lên, Đang tải về, Xung đột.
  + Thanh tiến trình `JProgressBar` cập nhật dung lượng đã truyền / tổng dung lượng theo thời gian thực.
  + Tab nhật ký hoạt động (Activity Log) tự động cuộn xuống dòng mới nhất.
  + Tích hợp đầy đủ các luồng Worker nền để tránh gây đơ giao diện chính.

### 2. Đánh giá ảnh hưởng (Impact Assessment)
- Hoàn thiện trải nghiệm người dùng phía Client, kết nối trực tiếp với `SyncClient` và `SyncEngine`.

### 3. Kết quả thử nghiệm và Bằng chứng thực thi
- Khởi chạy ClientGUI, thực hiện chọn thư mục, kết nối tới Server và bấm "Đồng bộ ngay". Giao diện phản hồi mượt mà, thanh tiến trình hiển thị chính xác % dữ liệu truyền.

---

### 4. Biên bản Thẩm định & Nghiệm thu Kỹ thuật (Verification Checklist)
- **Người thẩm định:** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Checklist nghiệm thu:**
  - [x] Giao diện trực quan, phối màu hài hòa và hiện đại.
  - [x] Cập nhật trạng thái bảng và thanh tiến trình qua SwingUtilities.invokeLater() được thực hiện bài bản, tránh lỗi race condition trên Event Dispatch Thread (EDT).
  - [x] Đã thử nghiệm tải lên và tải về thành công trực tiếp từ giao diện.
- **Quyết định (Status):** **VERIFIED & APPROVED FOR MERGE**
