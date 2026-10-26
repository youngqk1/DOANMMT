# Issue #4: Thiết kế Giao diện Client Dashboard & Trình điều khiển Đồng bộ GUI

- **Mã Issue:** #4
- **Người thực hiện (Assignee):** Trần Nguyễn Hoàng Quốc Khánh (MSSV: 2280601477)
- **Nhãn (Labels):** `frontend`, `gui`, `client`
- **Cột mốc (Milestone):** Tuần 2: Core Đồng bộ Đa Luồng & Quản lý Kho
- **Trạng thái:** Closed (Đã hoàn thành)

---

### 1. Mục tiêu công việc
Thiết kế giao diện đồ họa người dùng trực quan, chuyên nghiệp cho phía Client bằng Java Swing:
- Bảng điều khiển cấu hình kết nối: Địa chỉ máy chủ (Host IP), Cổng (Port), Tên định danh (Client ID), nút chọn thư mục đồng bộ cục bộ (JFileChooser).
- Thanh công cụ điều khiển: Nút "Đồng bộ ngay", checkbox chuyển đổi chế độ "Tự động đồng bộ theo thời gian thực", nút "Mở thư mục trên máy".
- Thanh tiến trình truyền dữ liệu (JProgressBar): Hiển thị tiến độ tải lên/tải xuống chi tiết theo dung lượng và phần trăm (0 - 100%).
- Bảng danh sách tệp tin hiển thị trực quan: Tên tệp, Đường dẫn, Kích thước, Trạng thái đồng bộ (được tô màu theo quy chuẩn: Đã đồng bộ - xanh lá, Đang tải lên - xanh dương, Đang tải về - xanh ngọc, Xung đột - đỏ cam).
- Tab nhật ký hoạt động (Activity Log) ghi nhận từng sự kiện với mốc thời gian thực.
- Đảm bảo an toàn cập nhật giao diện Swing thông qua `SwingUtilities.invokeLater()`.

### 2. Các tệp tin hiện thực
- `src/com/filesync/client/ClientGUI.java`

### 3. Tiêu chí nghiệm thu (Acceptance Criteria)
- [x] Giao diện hiển thị trực quan, thân thiện, không bị đơ khi đang truyền tệp lớn.
- [x] Bảng trạng thái cập nhật chính xác theo tiến trình đồng bộ.
- [x] Tùy chọn bật/tắt tự động đồng bộ hoạt động chính xác.
