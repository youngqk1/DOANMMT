package com.filesync;

import com.filesync.client.ClientGUI;
import com.filesync.common.Constants;
import com.filesync.server.ServerGUI;
import com.filesync.server.SyncServer;
import com.filesync.test.MultiClientSimulationTest;

import javax.swing.*;
import java.io.File;

/**
 * Lớp điều phối khởi chạy chính của Ứng dụng Đồng bộ Tệp tin
 * Hỗ trợ các chế độ chạy: Server GUI, Client GUI, Server Headless CLI và Kịch bản Test tự động
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class Main {
    public static void main(String[] args) {
        if (args.length > 0) {
            String mode = args[0].toLowerCase();
            switch (mode) {
                case "--server":
                case "-s":
                    startServerGUI();
                    return;

                case "--client":
                case "-c":
                    String clientId = (args.length > 1) ? args[1] : "Client-1";
                    String folderName = (args.length > 2) ? args[2] : "client_storage_1";
                    startClientGUI(clientId, folderName);
                    return;

                case "--test":
                case "-t":
                    MultiClientSimulationTest.main(new String[0]);
                    return;

                case "--server-cli":
                    startServerCLI();
                    return;
            }
        }

        // Nếu không có tham số dòng lệnh, hiển thị hộp thoại chọn chế độ
        showLauncherDialog();
    }

    private static void showLauncherDialog() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        String[] options = {
                "1. Khởi chạy Máy chủ (Server GUI)",
                "2. Khởi chạy Máy khách 1 (Client 1 GUI)",
                "3. Khởi chạy Máy khách 2 (Client 2 GUI)",
                "4. Chạy kịch bản Kiểm thử tự động (Simulation Test)"
        };

        String selected = (String) JOptionPane.showInputDialog(
                null,
                "Chọn phân hệ bạn muốn khởi chạy:\n(Đồ án cá nhân - Trần Nguyễn Hoàng Quốc Khánh - MSSV: 2280601477)",
                "FileSync System Launcher - Trần Nguyễn Hoàng Quốc Khánh",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (selected == null) return;

        if (selected.startsWith("1")) {
            startServerGUI();
        } else if (selected.startsWith("2")) {
            startClientGUI("Client-User1", "client_storage_1");
        } else if (selected.startsWith("3")) {
            startClientGUI("Client-User2", "client_storage_2");
        } else if (selected.startsWith("4")) {
            MultiClientSimulationTest.main(new String[0]);
        }
    }

    private static void startServerGUI() {
        SwingUtilities.invokeLater(() -> {
            File storageDir = new File("server_storage");
            ServerGUI gui = new ServerGUI(Constants.DEFAULT_PORT, storageDir);
            gui.setVisible(true);
        });
    }

    private static void startClientGUI(String clientId, String folderName) {
        SwingUtilities.invokeLater(() -> {
            File clientDir = new File(folderName);
            ClientGUI gui = new ClientGUI(Constants.DEFAULT_HOST, Constants.DEFAULT_PORT, clientId, clientDir);
            gui.setVisible(true);
        });
    }

    private static void startServerCLI() {
        File storageDir = new File("server_storage");
        SyncServer server = new SyncServer(Constants.DEFAULT_PORT, storageDir);
        try {
            server.start();
            System.out.println("Máy chủ Server CLI đang hoạt động. Nhấn Enter để dừng...");
            System.in.read();
            server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
