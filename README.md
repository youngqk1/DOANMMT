# ỨNG DỤNG ĐỒNG BỘ TỆP TIN (CLIENT-SERVER FILE SYNCHRONIZATION)
### BÁO CÁO ĐỒ ÁN KẾT THÚC HỌC PHẦN: LẬP TRÌNH MẠNG MÁY TÍNH
**TRƯỜNG ĐẠI HỌC CÔNG NGHỆ TP.HCM (HUTECH) – KHOA CÔNG NGHỆ THÔNG TIN**

- **Sinh viên thực hiện:** **Trần Nguyễn Hoàng Quốc Khánh**
- **Mã số sinh viên (MSSV):** **2280601477**
- **Lớp chuyên ngành:** **22DTHG6**
- **Hình thức thực hiện:** **Đồ án cá nhân (Individual Project)**

---

## 👤 1. BẢNG MA TRẬN ĐÓNG GÓP CÁ NHÂN (THEO MỤC 6 QUY TRÌNH GIT)

| MSSV | Họ và Tên | Công việc đã thực hiện | Merge Requests | Tỷ lệ đóng góp |
| :---: | :--- | :--- | :---: | :---: |
| **2280601477** | **Trần Nguyễn Hoàng Quốc Khánh** | • Thiết kế toàn bộ kiến trúc mạng Client - Server.<br>• Xây dựng Core Network Protocol (TCP Socket, framed message streaming, CommandType, FileMetadata).<br>• Hiện thực cơ chế tính toán và đối chiếu mã băm SHA-256, chống Path Traversal.<br>• Xây dựng động cơ đồng bộ 2 chiều SyncEngine và DirectoryWatcher (Java NIO WatchService).<br>• Tích hợp bộ đệm Debouncing (600ms) và cơ chế triệt tiêu phản xạ lặp (Suppression).<br>• Phát triển giải thuật phân giải đụng độ Optimistic Concurrency bảo toàn dữ liệu 100%.<br>• Hiện thực máy chủ đa luồng SyncServer và ServerStorageManager quản lý catalog & versions.<br>• Thiết kế toàn bộ giao diện đồ họa ClientGUI và ServerGUI bằng Java Swing.<br>• Xây dựng bộ kiểm thử tự động MultiClientSimulationTest (đạt 6/6 test cases 100%). | **!1, !2, !3, !4, !5, !6** | **100%** |

---

## 📋 2. QUẢN LÝ CÔNG VIỆC BẰNG ISSUE (TASK BOARD - MỤC 2)

| Mã Issue | Tiêu đề Issue | Người thực hiện (Assignee) | Nhãn (Labels) | Cột mốc (Milestone) | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **#1** | Xây dựng Core Network Protocol & Base Socket Framework | Trần Nguyễn Hoàng Quốc Khánh | `core-network`, `enhancement`, `backend` | Tuần 1: Kiến trúc & Giao thức mạng | **Closed** |
| **#2** | Hiện thực Động cơ Đồng bộ Tệp tin & WatchService | Trần Nguyễn Hoàng Quốc Khánh | `backend`, `enhancement`, `sync-engine` | Tuần 1: Kiến trúc & Giao thức mạng | **Closed** |
| **#3** | Xây dựng Máy chủ Đa luồng & Quản lý Kho lưu trữ | Trần Nguyễn Hoàng Quốc Khánh | `backend`, `multithreading`, `server` | Tuần 2: Core Đồng bộ & Đa luồng | **Closed** |
| **#4** | Thiết kế Giao diện Client Dashboard & Trình điều khiển GUI | Trần Nguyễn Hoàng Quốc Khánh | `frontend`, `gui`, `client` | Tuần 2: Core Đồng bộ & Đa luồng | **Closed** |
| **#5** | Xử lý Đụng độ phiên bản (Conflict) & Lịch sử Tệp tin (Versioning) | Trần Nguyễn Hoàng Quốc Khánh | `backend`, `conflict-resolution`, `versioning` | Tuần 3: GUI Monitor & Kiểm thử | **Closed** |
| **#6** | Xây dựng Server GUI Monitor & Kịch bản Kiểm thử tự động | Trần Nguyễn Hoàng Quốc Khánh | `frontend`, `gui`, `testing` | Tuần 3: GUI Monitor & Kiểm thử | **Closed** |

---

## 🔀 3. CHIẾN LƯỢC PHÂN NHÁNH VÀ MERGE REQUEST (MỤC 3 & MỤC 5)

Hệ thống tuân thủ nghiêm ngặt quy tắc rẽ nhánh theo định dạng `feature/<MSSV>-issue-<Số_Issue>-<Tên_ngắn_gọn>`:

- `feature/2280601477-issue-1-core-network-protocol` $\rightarrow$ Merge Request **!1** (Verified & Merged)
- `feature/2280601477-issue-2-file-sync-engine` $\rightarrow$ Merge Request **!2** (Verified & Merged)
- `feature/2280601477-issue-3-server-storage-manager` $\rightarrow$ Merge Request **!3** (Verified & Merged)
- `feature/2280601477-issue-4-client-gui-dashboard` $\rightarrow$ Merge Request **!4** (Verified & Merged)
- `feature/2280601477-issue-5-conflict-resolution-versioning` $\rightarrow$ Merge Request **!5** (Verified & Merged)
- `feature/2280601477-issue-6-server-gui-and-simulation-test` $\rightarrow$ Merge Request **!6** (Verified & Merged)

---

## 🏗️ 4. KIẾN TRÚC HỆ THỐNG VÀ ĐẶC TẢ GIAO THỨC

```
                      ┌──────────────────────────────────────────────┐
                      │              SYNC SERVER (JAVA)              │
                      │  - ServerSocket (Port 9999)                  │
                      │  - ThreadPool (ExecutorService)              │
                      │  - ServerStorageManager (Catalog + Versions) │
                      │  - ServerGUI Monitor (Dashboard)             │
                      └──────────────┬────────────────┬──────────────┘
                                     │                │
            Kênh Điều khiển & Dữ liệu│                │Kênh Phát tán Sự kiện
           (Command & Data Streaming)│                │(Real-time Push Notifications)
                                     ▼                ▼
     ┌────────────────────────────────────┐      ┌────────────────────────────────────┐
     │        CLIENT 1 (ALPHA)            │      │         CLIENT 2 (BETA)            │
     │  - SyncEngine (2-Way Full Sync)    │      │  - SyncEngine (2-Way Full Sync)    │
     │  - DirectoryWatcher (NIO Watcher)  │      │  - DirectoryWatcher (NIO Watcher)  │
     │  - Conflict Resolver (Safe Rename) │      │  - Conflict Resolver (Safe Rename) │
     │  - ClientGUI Dashboard (Swing)     │      │  - ClientGUI Dashboard (Swing)     │
     │  - Thư mục: client_storage_1/      │      │  - Thư mục: client_storage_2/      │
     └────────────────────────────────────┘      └────────────────────────────────────┘
```

### Điểm nổi bật về mặt công nghệ:
1. **Giao thức mạng nhị phân phân khung (Framed Binary Protocol)**: Truyền tải dữ liệu theo từng khối (Chunking 64 KB), cho phép truyền tệp có dung lượng bất kỳ mà không bị quá tải RAM (OutOfMemoryError).
2. **Kiểm tra tính toàn vẹn tuyệt đối (SHA-256 Checksum)**: Mỗi tệp tin trước và sau khi truyền tải đều được băm SHA-256 đối chiếu 100%. Tránh truyền trùng lặp dữ liệu (Deduplication).
3. **Phát hiện sự kiện tệp thời gian thực (Java NIO WatchService)**: Bắt ngay lập tức khi người dùng thêm, sửa, xóa file trong thư mục làm việc.
4. **Bộ đệm chống lặp (Debouncing) & Chống phản xạ lặp (Suppression)**: Bỏ qua các sự kiện rung do hệ điều hành tạo ra trong vòng 600ms và không kích hoạt upload ngược lại khi đang tải file từ server về.
5. **Kiểm soát phiên bản và đụng độ an toàn (Optimistic Concurrency Control)**: Khi 2 client cùng sửa 1 tệp, hệ thống phát hiện đụng độ và tự động tách nhánh thành `[tên]_conflict_[client]_[thời_gian].[đuôi]`, bảo đảm **100% không mất dữ liệu của người dùng**.

---

## 🧪 5. KẾT QUẢ KIỂM THỬ THỰC NGHIỆM (SIMULATION TEST)

Chương trình kiểm thử tự động `MultiClientSimulationTest` thực thi 6 ca kiểm thử độc lập:

| STT | Kịch bản Kiểm thử (Test Case) | Kết quả kỳ vọng | Thực tế | Trạng thái |
| :---: | :--- | :--- | :---: | :---: |
| **TC1** | Tạo và đồng bộ tệp văn bản mới từ Client 1 lên Server | Tệp xuất hiện trên Server, SHA-256 khớp 100% | Đã khớp 100% | **PASSED** |
| **TC2** | Client 2 đồng bộ kéo tệp từ Server về máy | Tệp xuất hiện tại thư mục Client 2 nguyên vẹn | Nội dung đầy đủ | **PASSED** |
| **TC3** | Truyền khối nhị phân lớn 180 KB (3 Chunks x 64KB) | Khối dữ liệu truyền nguyên vẹn, SHA-256 đối chiếu đúng | SHA khớp 100% | **PASSED** |
| **TC4** | Chỉnh sửa tệp và kiểm tra cơ chế tăng phiên bản & lưu trữ | Server tăng version (v1 $\rightarrow$ v2), lưu bản cũ vào `.versions/` | v2 + lưu trữ v1 | **PASSED** |
| **TC5** | Mô phỏng 2 Client cùng sửa một tệp (Offline Conflict) | Server báo Conflict, Client tách file `_conflict_`, không mất dữ liệu | Tách file an toàn | **PASSED** |
| **TC6** | Xóa tệp tin trên Client 1 và đồng bộ xóa lên Server | Tệp bị xóa khỏi Server và phát tín hiệu NOTIFY xóa đồng bộ các Client | Đã xóa đồng bộ | **PASSED** |

**Tổng kết:** **6/6 ca kiểm thử hoàn thành xuất sắc (100.0% PASSED).**

---

## 🚀 6. HƯỚNG DẪN BIÊN DỊCH VÀ KHỞI CHẠY (1-CLICK)

### 1. Biên dịch toàn bộ mã nguồn:
Nhấp đúp chuột vào tệp:
```cmd
compile.bat
```
*(Mã nguồn sẽ được biên dịch bằng `javac -encoding UTF-8` và xuất bytecode ra thư mục `bin/`)*

### 2. Khởi chạy Máy chủ (Server):
Nhấp đúp chuột vào tệp:
```cmd
run_server.bat
```
*(Cửa sổ Server GUI Dashboard sẽ xuất hiện, bấm nút "Khởi động Server" để bắt đầu lắng nghe)*

### 3. Khởi chạy Máy khách (Clients):
- Khởi chạy Client 1: Nhấp đúp chuột vào `run_client1.bat` (Thư mục làm việc: `client_storage_1`)
- Khởi chạy Client 2: Nhấp đúp chuột vào `run_client2.bat` (Thư mục làm việc: `client_storage_2`)
- Bấm nút **"Kết nối Server"** trên từng Client để bắt đầu đồng bộ dữ liệu hai chiều tự động.

### 4. Chạy kịch bản Kiểm thử tự động:
Nhấp đúp chuột vào tệp:
```cmd
run_test.bat
```

---

## 📤 7. HƯỚNG DẪN CẤU HÌNH GIT VÀ ĐẨY LÊN GITHUB / GITLAB

Dự án đã được khởi tạo sẵn sàng toàn bộ lịch sử commit, phân nhánh feature và merge request chuẩn Conventional Commits. Để đẩy toàn bộ lên kho chứa từ xa (GitHub/GitLab):

```bash
# 1. Thêm địa chỉ Remote cá nhân
git remote add origin https://github.com/<tai-khoan>/<ten-repo>.git

# 2. Đẩy nhánh chính main lên GitHub
git push -u origin main

# 3. Đẩy toàn bộ các nhánh tính năng (Feature branches) lên GitHub
git push --all origin
```
