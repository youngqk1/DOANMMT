# Merge Request !6: Server GUI Monitor & Automated Simulation Tests

- **Nguồn (Source Branch):** `feature/2280601477-issue-6-server-gui-and-simulation-test`
- **Đích (Target Branch):** `main`
- **Người thực hiện (Author):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Hình thức thực hiện:** Đồ án cá nhân (Individual Project)
- **Trạng thái:** MERGED
- **Liên kết Issue:** Closes #6

---

### 1. Nội dung thay đổi (Description)
- Xây dựng bảng điều khiển quản trị máy chủ `ServerGUI`: hiển thị trạng thái máy chủ, danh sách Client đang kết nối, kho tệp tin đang lưu trữ, xem lịch sử phiên bản tệp và nhật ký thời gian thực.
- Xây dựng bộ kiểm thử mô phỏng tự động `MultiClientSimulationTest` bao quát toàn bộ 6 ca kiểm thử nghiệp vụ:
  1. Tải lên tệp mới và đối chiếu mã băm SHA-256.
  2. Đồng bộ tải tệp từ máy chủ về máy khách khác.
  3. Truyền tệp nhị phân lớn 180 KB qua nhiều khối (Chunking) 64KB.
  4. Quản lý tăng phiên bản v1 -> v2 và lưu trữ lịch sử bản sao.
  5. Xử lý đụng độ sửa đổi đồng thời (Offline Conflict).
  6. Xóa tệp tin và phát tán trạng thái xóa qua mạng.
- Xây dựng các tệp kịch bản thực thi một chạm (.bat): `compile.bat`, `run_server.bat`, `run_client1.bat`, `run_client2.bat`, `run_test.bat`.
- Hiện thực lớp khởi chạy trung tâm `Main.java` hỗ trợ điều hướng menu GUI và tham số dòng lệnh.

### 2. Đánh giá ảnh hưởng (Impact Assessment)
- Hoàn thiện toàn bộ dự án, cung cấp công cụ kiểm thử tự động phục vụ chấm điểm và bảo vệ đồ án kết thúc môn.

### 3. Kết quả thử nghiệm và Bằng chứng thực thi
- Chạy kiểm thử tự động `MultiClientSimulationTest`: Đạt 6/6 ca kiểm thử (100.0% PASSED).
- Biên dịch dự án bằng `compile.bat`: Thành công 100%, không phát sinh cảnh báo hay lỗi.

---

### 4. Biên bản Thẩm định & Nghiệm thu Kỹ thuật (Verification Checklist)
- **Người thẩm định:** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Checklist nghiệm thu:**
  - [x] Bộ kiểm thử tự động toàn diện, chạy mượt mà, đạt 6/6 ca kiểm thử 100.0%.
  - [x] Các tệp kịch bản batch (.bat) khởi chạy nhanh chóng và tiện lợi cho giảng viên khi chấm bài.
  - [x] ServerGUI và ClientGUI tương tác ổn định qua giao thức mạng TCP Socket.
- **Quyết định (Status):** **VERIFIED & APPROVED FOR MERGE**
