# Merge Request !5: Conflict Resolution & File Versioning History

- **Nguồn (Source Branch):** `feature/2280601477-issue-5-conflict-resolution-versioning`
- **Đích (Target Branch):** `main`
- **Người thực hiện (Author):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Hình thức thực hiện:** Đồ án cá nhân (Individual Project)
- **Trạng thái:** MERGED
- **Liên kết Issue:** Closes #5

---

### 1. Nội dung thay đổi (Description)
- Xây dựng cơ chế phát hiện đụng độ sửa đổi tệp dựa trên kiểm soát phiên bản lạc quan (Optimistic Concurrency Control): So sánh version của Client đang giữ với version trên Server.
- Xây dựng giải thuật phân giải đụng độ (Conflict Resolution Policy): Đổi tên tệp cục bộ thành `[tên]_conflict_[client]_[thời_gian].[đuôi]`, bảo toàn dữ liệu gốc, kéo tệp từ Server về và tải tệp conflict lên Server để cùng đối chiếu.
- Tích hợp hệ thống lưu trữ đa phiên bản (File Versioning) vào thư mục `.versions/` và hỗ trợ truy vấn danh sách phiên bản cũ.

### 2. Đánh giá ảnh hưởng (Impact Assessment)
- Giải quyết bài toán then chốt trong hệ thống đồng bộ tệp tin phân tán, bảo đảm an toàn dữ liệu 100% khi người dùng làm việc offline hoặc cùng chỉnh sửa một tài liệu.

### 3. Kết quả thử nghiệm và Bằng chứng thực thi
- Thực nghiệm kịch bản 2 client sửa cùng 1 tệp tin trong khi offline. Khi đồng bộ, Server phát hiện xung đột và Client tự động sinh ra tệp có hậu tố `_conflict_`, không có bất kỳ byte dữ liệu nào bị ghi đè mất mát.

---

### 4. Biên bản Thẩm định & Nghiệm thu Kỹ thuật (Verification Checklist)
- **Người thẩm định:** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Checklist nghiệm thu:**
  - [x] Giải thuật xử lý xung đột hoạt động chính xác theo chuẩn của Dropbox/OneDrive.
  - [x] Tệp conflict được đặt tên rõ ràng kèm định danh người sửa và timestamp.
  - [x] Bản lưu lịch sử phiên bản `.versions/` được sao lưu nguyên vẹn, đối chiếu mã SHA-256 chính xác.
- **Quyết định (Status):** **VERIFIED & APPROVED FOR MERGE**
