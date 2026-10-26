# Issue #6: Xây dựng Server GUI Monitor & Kịch bản Kiểm thử tự động

- **Mã Issue:** #6
- **Người thực hiện (Assignee):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Nhãn (Labels):** `frontend`, `gui`, `testing`
- **Cột mốc (Milestone):** Tuần 3: Đụng độ Phiên Bản, GUI Monitor & Kiểm Thử
- **Trạng thái:** Closed (Đã hoàn thành)

---

### 1. Mục tiêu công việc
Hoàn thiện giao diện giám sát quản trị Server, kịch bản kiểm thử tự động và bộ kịch bản thực thi một chạm:
- Xây dựng `ServerGUI`: Bảng điều khiển quản trị máy chủ hiển thị số Client trực tuyến, danh mục tệp tin quản lý, các phiên bản lịch sử và nhật ký hoạt động thời gian thực.
- Xây dựng kịch bản kiểm thử mô phỏng tự động `MultiClientSimulationTest` bao gồm 6 ca kiểm thử độc lập:
  1. Upload tệp mới từ Client 1 lên Server.
  2. Đồng bộ tải tệp từ Server về Client 2.
  3. Truyền tệp nhị phân kích thước lớn theo khối (180 KB qua 3 chunks x 64KB) và đối chiếu SHA-256.
  4. Sửa tệp và kiểm tra nâng số phiên bản (v1 -> v2) cùng cơ chế lưu trữ lịch sử.
  5. Mô phỏng đụng độ sửa tệp ngoại tuyến (Offline Conflict) và phân giải an toàn tách file `_conflict_`.
  6. Xóa tệp tin và đồng bộ trạng thái xóa trên toàn hệ thống.
- Xây dựng các tệp kịch bản batch: `compile.bat`, `run_server.bat`, `run_client1.bat`, `run_client2.bat`, `run_test.bat`.
- Hiện thực lớp khởi chạy trung tâm `Main.java` hỗ trợ cả chế độ đồ họa và tham số dòng lệnh CLI.

### 2. Các tệp tin hiện thực
- `src/com/filesync/server/ServerGUI.java`
- `src/com/filesync/test/MultiClientSimulationTest.java`
- `src/com/filesync/Main.java`
- `compile.bat`, `run_server.bat`, `run_client1.bat`, `run_client2.bat`, `run_test.bat`

### 3. Tiêu chí nghiệm thu (Acceptance Criteria)
- [x] Kịch bản kiểm thử chạy tự động đạt 100% (6/6 ca kiểm thử hoàn thành xuất sắc).
- [x] Biên dịch và khởi chạy 1-click mượt mà bằng các file script .bat.
- [x] ServerGUI phản ánh đầy đủ thông số hoạt động của hệ thống mạng.
