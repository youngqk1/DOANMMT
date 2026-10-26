package com.filesync.test;

import com.filesync.client.SyncClient;
import com.filesync.client.SyncEngine;
import com.filesync.common.*;
import com.filesync.server.SyncServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * Kịch bản kiểm thử mô phỏng tự động đa Client (Multi-client Automated Simulation Test)
 * Kiểm thử toàn diện 6 ca kiểm thử: Upload, Download, Truyền khối lớn (Chunking),
 * Tăng phiên bản (Versioning), Đụng độ đồng thời (Conflict Resolution) và Xóa tệp (Delete)
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class MultiClientSimulationTest {
    private static final int TEST_PORT = 9991;
    private static final String HOST = "127.0.0.1";

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println(" BẮT ĐẦU KIỂM THỬ MÔ PHỎNG ĐA CLIENT - HỆ THỐNG ĐỒNG BỘ TỆP TIN");
        System.out.println(" Học phần: Lập trình mạng máy tính - SVTH: Trần Nguyễn Hoàng Quốc Khánh (2280601477)");
        System.out.println("======================================================================\n");

        File testRootDir = new File("build_test_env");
        File serverStorage = new File(testRootDir, "server_storage");
        File client1Storage = new File(testRootDir, "client1_storage");
        File client2Storage = new File(testRootDir, "client2_storage");

        // Dọn dẹp thư mục kiểm thử cũ
        FileUtil.deleteRecursively(testRootDir);
        testRootDir.mkdirs();
        serverStorage.mkdirs();
        client1Storage.mkdirs();
        client2Storage.mkdirs();

        SyncServer server = new SyncServer(TEST_PORT, serverStorage);
        SyncClient client1 = new SyncClient(HOST, TEST_PORT, "Client-Alpha");
        SyncClient client2 = new SyncClient(HOST, TEST_PORT, "Client-Beta");

        SyncEngine engine1 = null;
        SyncEngine engine2 = null;

        int passed = 0;
        int totalTests = 6;

        try {
            // 1. Khởi động Máy chủ
            server.start();
            Thread.sleep(300);

            // 2. Kết nối Client 1 và Client 2
            client1.connect();
            client2.connect();

            engine1 = new SyncEngine(client1Storage, client1);
            engine2 = new SyncEngine(client2Storage, client2);

            engine1.start();
            engine2.start();

            System.out.println("[SETUP] Máy chủ và 2 Client đã sẵn sàng trên cổng " + TEST_PORT + "\n");

            // -------------------------------------------------------------
            // TEST CASE 1: Tải lên tệp mới từ Client 1 lên Server
            // -------------------------------------------------------------
            System.out.println("▶ [TEST 1] Tạo và đồng bộ tệp văn bản mới từ Client 1 lên Server...");
            File c1File = new File(client1Storage, "tai_lieu_nhom14.txt");
            String content1 = "Báo cáo Đồ án Lập trình mạng máy tính - Nhóm 14\nĐề tài: Đồng bộ tệp tin Client-Server.";
            Files.write(c1File.toPath(), content1.getBytes(StandardCharsets.UTF_8));

            boolean sync1Ok = engine1.syncAll();
            File serverFile = new File(serverStorage, "tai_lieu_nhom14.txt");
            boolean tc1Passed = sync1Ok && serverFile.exists() &&
                    FileUtil.calculateSHA256(c1File).equalsIgnoreCase(FileUtil.calculateSHA256(serverFile));

            if (tc1Passed) {
                System.out.println("  ==> ĐẠT (PASSED): Tệp đã tải lên Server, SHA-256 trùng khớp 100%!\n");
                passed++;
            } else {
                System.err.println("  ==> THẤT BẠI (FAILED): Lỗi tải tệp lên máy chủ!\n");
            }

            // -------------------------------------------------------------
            // TEST CASE 2: Đồng bộ tệp từ Server về Client 2
            // -------------------------------------------------------------
            System.out.println("▶ [TEST 2] Client 2 thực hiện đồng bộ kéo tệp từ Server về máy...");
            boolean sync2Ok = engine2.syncAll();
            File c2File = new File(client2Storage, "tai_lieu_nhom14.txt");
            boolean tc2Passed = sync2Ok && c2File.exists() &&
                    FileUtil.calculateSHA256(c2File).equalsIgnoreCase(FileUtil.calculateSHA256(serverFile));

            if (tc2Passed) {
                System.out.println("  ==> ĐẠT (PASSED): Client 2 đã tải tệp về thành công, nội dung nguyên vẹn!\n");
                passed++;
            } else {
                System.err.println("  ==> THẤT BẠI (FAILED): Client 2 không tải được tệp từ Server!\n");
            }

            // -------------------------------------------------------------
            // TEST CASE 3: Truyền tệp nhị phân kích thước lớn theo khối (Chunking)
            // -------------------------------------------------------------
            System.out.println("▶ [TEST 3] Truyền khối nhị phân lớn 180 KB (3 Chunks x 64KB) và đối chiếu SHA-256...");
            File c1Binary = new File(client1Storage, "du_lieu_nhi_phan.bin");
            byte[] binaryData = new byte[180 * 1024]; // 180 KB
            Arrays.fill(binaryData, (byte) 0x5A);
            try (FileOutputStream fos = new FileOutputStream(c1Binary)) {
                fos.write(binaryData);
            }

            engine1.syncAll();
            engine2.syncAll();

            File c2Binary = new File(client2Storage, "du_lieu_nhi_phan.bin");
            String shaSrc = FileUtil.calculateSHA256(c1Binary);
            String shaDst = FileUtil.calculateSHA256(c2Binary);
            boolean tc3Passed = c2Binary.exists() && shaSrc.equalsIgnoreCase(shaDst);

            if (tc3Passed) {
                System.out.println("  ==> ĐẠT (PASSED): Tệp nhị phân 180 KB đã truyền thành công qua các chunk! SHA: " + shaSrc.substring(0, 16) + "...\n");
                passed++;
            } else {
                System.err.println("  ==> THẤT BẠI (FAILED): Sai lệch mã băm khi truyền chunk nhị phân!\n");
            }

            // -------------------------------------------------------------
            // TEST CASE 4: Chỉnh sửa tệp và quản lý phiên bản (File Versioning)
            // -------------------------------------------------------------
            System.out.println("▶ [TEST 4] Chỉnh sửa tệp và kiểm tra cơ chế tăng phiên bản & lưu trữ lịch sử...");
            String content2 = content1 + "\nCập nhật bổ sung phiên bản 2 bởi Client 2.";
            Files.write(c2File.toPath(), content2.getBytes(StandardCharsets.UTF_8));
            c2File.setLastModified(System.currentTimeMillis() + 5000);

            engine2.syncAll();
            FileMetadata sMeta = server.getStorageManager().getMetadata("tai_lieu_nhom14.txt");
            List<String> history = server.getStorageManager().getVersionHistory("tai_lieu_nhom14.txt");

            boolean tc4Passed = sMeta != null && sMeta.getVersion() >= 2 && !history.isEmpty();
            if (tc4Passed) {
                System.out.println("  ==> ĐẠT (PASSED): Phiên bản nâng lên v" + sMeta.getVersion() + ", đã lưu trữ bản cũ: " + history.get(0) + "\n");
                passed++;
            } else {
                System.err.println("  ==> THẤT BẠI (FAILED): Quản lý phiên bản không chính xác!\n");
            }

            // -------------------------------------------------------------
            // TEST CASE 5: Xử lý đụng độ đồng thời (Conflict Resolution)
            // -------------------------------------------------------------
            System.out.println("▶ [TEST 5] Mô phỏng 2 Client cùng sửa một tệp (Offline Conflict) -> Phân giải đụng độ bảo toàn dữ liệu...");
            File conflictFile1 = new File(client1Storage, "so_tay.txt");
            File conflictFile2 = new File(client2Storage, "so_tay.txt");

            Files.write(conflictFile1.toPath(), "Nội dung gốc ban đầu".getBytes(StandardCharsets.UTF_8));
            engine1.syncAll();
            engine2.syncAll(); // Đồng bộ trạng thái ban đầu

            // Tạm thời tắt auto sync trên Client 2 để mô phỏng trạng thái Offline / song song
            engine2.setAutoSyncEnabled(false);

            // Cả 2 cùng sửa với nội dung khác nhau khi chưa đồng bộ
            Files.write(conflictFile1.toPath(), "Sửa đổi nhánh 1 - Alpha".getBytes(StandardCharsets.UTF_8));
            Files.write(conflictFile2.toPath(), "Sửa đổi nhánh 2 - Beta".getBytes(StandardCharsets.UTF_8));
            conflictFile1.setLastModified(System.currentTimeMillis() + 10000);
            conflictFile2.setLastModified(System.currentTimeMillis() + 10000);

            // Client 1 đẩy trước lên Server (Server lên v2)
            engine1.syncAll();

            // Client 2 kích hoạt đồng bộ (dựa trên bản cũ v1) -> Server phát hiện conflict!
            engine2.syncAll();

            // Bật lại auto-sync cho Client 2
            engine2.setAutoSyncEnabled(true);

            // Kiểm tra xem có sinh ra tệp _conflict_ không
            File[] c2Files = client2Storage.listFiles((dir, name) -> name.contains(Constants.CONFLICT_PREFIX));
            boolean tc5Passed = c2Files != null && c2Files.length > 0;

            if (tc5Passed) {
                System.out.println("  ==> ĐẠT (PASSED): Phát hiện đụng độ thành công! Tệp được bảo toàn dạng: " + c2Files[0].getName() + "\n");
                passed++;
            } else {
                System.err.println("  ==> THẤT BẠI (FAILED): Không phát hiện hoặc không phân giải được đụng độ!\n");
            }

            // -------------------------------------------------------------
            // TEST CASE 6: Xóa tệp tin và đồng bộ trạng thái xóa (Delete)
            // -------------------------------------------------------------
            System.out.println("▶ [TEST 6] Xóa tệp tin trên Client 1 và kiểm tra đồng bộ xóa lên Server...");
            boolean deleted = client1.deleteFileOnServer("du_lieu_nhi_phan.bin");
            File serverBin = new File(serverStorage, "du_lieu_nhi_phan.bin");
            boolean tc6Passed = deleted && !serverBin.exists();

            if (tc6Passed) {
                System.out.println("  ==> ĐẠT (PASSED): Server đã xử lý xóa tệp tin thành công!\n");
                passed++;
            } else {
                System.err.println("  ==> THẤT BẠI (FAILED): Lỗi xử lý xóa tệp tin!\n");
            }

        } catch (Exception e) {
            System.err.println("Lỗi ngoại lệ trong quá trình chạy kiểm thử: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Đóng các kết nối
            if (engine1 != null) engine1.stop();
            if (engine2 != null) engine2.stop();
            client1.disconnect();
            client2.disconnect();
            server.stop();

            // Dọn dẹp thư mục kiểm thử
            FileUtil.deleteRecursively(testRootDir);

            System.out.println("======================================================================");
            System.out.println(String.format(" KẾT QUẢ KIỂM THỬ: %d/%d CA KIỂM THỬ HOÀN THÀNH XUẤT SẮC (%.1f%%)",
                    passed, totalTests, (passed * 100.0 / totalTests)));
            System.out.println("======================================================================");
        }
    }
}
