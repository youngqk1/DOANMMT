# Merge Request !1: Core Network Protocol & Base Socket Framework

- **Nguồn (Source Branch):** `feature/2280601477-issue-1-core-network-protocol`
- **Đích (Target Branch):** `main`
- **Người thực hiện (Author):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Hình thức thực hiện:** Đồ án cá nhân (Individual Project)
- **Trạng thái:** MERGED
- **Liên kết Issue:** Closes #1

---

### 1. Nội dung thay đổi (Description)
Triển khai bộ thư viện tầng mạng cơ sở và giao thức truyền thông khung thông điệp (Framed Protocol):
- Xây dựng định dạng bản tin `Message` hỗ trợ truyền Header và Payload nhị phân/chuỗi UTF-8.
- Hiện thực bảng mã lệnh `CommandType` chuẩn hóa tương tác Client - Server.
- Xây dựng lớp mô hình dữ liệu `FileMetadata` lưu trữ thông tin tệp tin, tính toán hàm băm SHA-256.
- Xây dựng lớp tiện ích `FileUtil` phục vụ quét thư mục đệ quy, kiểm tra chống tấn công Path Traversal.

### 2. Đánh giá ảnh hưởng (Impact Assessment)
- Đây là module nền tảng lõi (Core Framework). Các module khác (Server, Client, SyncEngine) sẽ trực tiếp sử dụng các cấu trúc gói tin này.
- Không gây ảnh hưởng tiêu cực hay xung đột với các thành phần khác.

### 3. Kết quả thử nghiệm và Bằng chứng thực thi
- Đã chạy kiểm thử cục bộ: Đóng gói và giải mã khung bản tin Message qua mảng byte thành công 100%.
- Kiểm tra mã SHA-256 đối chiếu chuẩn xác với các tệp tin mẫu.

---

### 4. Biên bản Thẩm định & Nghiệm thu Kỹ thuật (Verification Checklist)
- **Người thẩm định:** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Checklist nghiệm thu:**
  - [x] Đóng gói và giải mã khung bản tin Message thành công qua Socket InputStream/OutputStream.
  - [x] Hàm chống tấn công Path Traversal hoạt động chuẩn xác với `getCanonicalFile()`.
  - [x] Biên dịch sạch sẽ, không có cảnh báo hay lỗi cú pháp.
- **Quyết định (Status):** **VERIFIED & APPROVED FOR MERGE**
