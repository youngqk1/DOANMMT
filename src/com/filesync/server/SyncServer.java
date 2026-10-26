package com.filesync.server;

import com.filesync.common.*;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Máy chủ đồng bộ tệp tin đa luồng (Multi-threaded Synchronization Server)
 * Quản lý các phiên kết nối Client, phát tán sự kiện thời gian thực (Broadcast)
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class SyncServer {
    private final int port;
    private final File storageDir;
    private final ServerStorageManager storageManager;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;

    // Callbacks phục vụ giao diện đồ họa (GUI)
    private Consumer<String> logListener;
    private Runnable clientStateListener;
    private Runnable fileCatalogListener;

    public SyncServer(int port, File storageDirectory) {
        this.port = port;
        this.storageDir = storageDirectory;
        this.storageManager = new ServerStorageManager(storageDirectory);
    }

    public int getPort() { return port; }
    public File getStorageDir() { return storageDir; }
    public ServerStorageManager getStorageManager() { return storageManager; }
    public List<ClientHandler> getActiveClients() { return Collections.unmodifiableList(clients); }
    public boolean isRunning() { return isRunning; }

    public void setLogListener(Consumer<String> listener) { this.logListener = listener; }
    public void setClientStateListener(Runnable listener) { this.clientStateListener = listener; }
    public void setFileCatalogListener(Runnable listener) { this.fileCatalogListener = listener; }

    public synchronized void start() throws IOException {
        if (isRunning) return;

        serverSocket = new ServerSocket(port);
        isRunning = true;
        log(String.format("Máy chủ Sync Server đã khởi động trên cổng %d", port));
        log(String.format("Thư mục lưu trữ máy chủ: %s", storageDir.getAbsolutePath()));

        // Chạy vòng lặp tiếp nhận kết nối trên luồng riêng
        threadPool.execute(this::acceptLoop);
    }

    private void acceptLoop() {
        while (isRunning) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, this, storageManager);
                clients.add(handler);
                threadPool.execute(handler);
            } catch (SocketException e) {
                if (!isRunning) break;
                log("Lỗi tiếp nhận kết nối Socket: " + e.getMessage());
            } catch (IOException e) {
                if (isRunning) {
                    log("Lỗi I/O Server Socket: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;

        // Đóng các kết nối Client hiện tại
        for (ClientHandler client : clients) {
            client.close();
        }
        clients.clear();

        // Đóng ServerSocket
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}

        log("Máy chủ Sync Server đã dừng hoạt động.");
        onClientStateChanged();
    }

    public void removeClient(ClientHandler handler) {
        clients.remove(handler);
        onClientStateChanged();
    }

    /**
     * Phát tán thông điệp cập nhật đến toàn bộ các kênh NOTIFY của các Client khác trong mạng
     */
    public void broadcastNotification(Message message, ClientHandler senderToExclude) {
        String senderBaseId = (senderToExclude != null) ? senderToExclude.getBaseClientId() : "";
        for (ClientHandler client : clients) {
            // Chỉ gửi thông báo tới các kênh đăng ký NOTIFY và bỏ qua chính client vừa phát sinh thay đổi
            if (client.isNotifyChannel() && !client.getBaseClientId().equalsIgnoreCase(senderBaseId)) {
                try {
                    client.sendMessage(message);
                } catch (IOException e) {
                    log(String.format("Không thể gửi thông báo đến [%s]: %s", client.getClientId(), e.getMessage()));
                }
            }
        }
    }

    public void log(String msg) {
        String logEntry = String.format("[%s] %s", FileUtil.formatTimestamp(System.currentTimeMillis()), msg);
        System.out.println(logEntry);
        if (logListener != null) {
            logListener.accept(logEntry);
        }
    }

    public void onClientStateChanged() {
        if (clientStateListener != null) {
            clientStateListener.run();
        }
    }

    public void onFileCatalogChanged() {
        if (fileCatalogListener != null) {
            fileCatalogListener.run();
        }
    }
}
