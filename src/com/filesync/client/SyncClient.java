package com.filesync.client;

import com.filesync.common.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Tầng mạng Client xử lý giao tiếp TCP Socket 2 kênh (Command Channel & Push Notification Channel)
 * Thực hiện tải lên, tải xuống theo khối (Chunking), đối chiếu SHA-256 và lắng nghe thay đổi
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class SyncClient {
    private final String host;
    private final int port;
    private final String clientId;

    private Socket cmdSocket;
    private DataInputStream cmdIn;
    private DataOutputStream cmdOut;

    private Socket notifySocket;
    private DataInputStream notifyIn;
    private DataOutputStream notifyOut;

    private Thread notifyThread;
    private volatile boolean isConnected = false;

    private Consumer<String> logListener;
    private Consumer<FileMetadata> changeNotificationListener;

    public SyncClient(String host, int port, String clientId) {
        this.host = host;
        this.port = port;
        this.clientId = clientId;
    }

    public String getClientId() { return clientId; }
    public boolean isConnected() { return isConnected; }

    public void setLogListener(Consumer<String> listener) { this.logListener = listener; }
    public void setChangeNotificationListener(Consumer<FileMetadata> listener) { this.changeNotificationListener = listener; }

    public synchronized void connect() throws IOException {
        if (isConnected) return;

        // 1. Kết nối kênh điều khiển chính (Command Channel)
        cmdSocket = new Socket(host, port);
        cmdIn = new DataInputStream(new BufferedInputStream(cmdSocket.getInputStream()));
        cmdOut = new DataOutputStream(new BufferedOutputStream(cmdSocket.getOutputStream()));

        Message hsCmd = new Message(CommandType.HANDSHAKE, "OK", "", clientId + ":CMD");
        hsCmd.writeTo(cmdOut);
        Message hsAck = Message.readFrom(cmdIn);
        if (hsAck.getCommand() != CommandType.HANDSHAKE_ACK) {
            throw new IOException("Server từ chối kết nối kênh điều khiển: " + hsAck.getMessage());
        }

        // 2. Kết nối kênh nhận thông báo phát tán thời gian thực (Push Notification Channel)
        notifySocket = new Socket(host, port);
        notifyIn = new DataInputStream(new BufferedInputStream(notifySocket.getInputStream()));
        notifyOut = new DataOutputStream(new BufferedOutputStream(notifySocket.getOutputStream()));

        Message hsNotify = new Message(CommandType.HANDSHAKE, "OK", "", clientId + ":NOTIFY");
        hsNotify.writeTo(notifyOut);
        Message hsNotifyAck = Message.readFrom(notifyIn);
        if (hsNotifyAck.getCommand() != CommandType.HANDSHAKE_ACK) {
            throw new IOException("Server từ chối kết nối kênh thông báo: " + hsNotifyAck.getMessage());
        }

        isConnected = true;
        log(String.format("Kết nối thành công đến Sync Server [%s:%d]", host, port));

        // Khởi chạy luồng đón nhận thông báo từ Server
        notifyThread = new Thread(this::listenNotifications, "NotifyListener-" + clientId);
        notifyThread.setDaemon(true);
        notifyThread.start();
    }

    private void listenNotifications() {
        while (isConnected) {
            try {
                Message msg = Message.readFrom(notifyIn);
                if (msg == null) break;

                if (msg.getCommand() == CommandType.NOTIFY_CHANGE) {
                    FileMetadata meta = FileMetadata.decode(msg.getPayload());
                    if (meta != null && changeNotificationListener != null) {
                        changeNotificationListener.accept(meta);
                    }
                }
            } catch (IOException e) {
                if (isConnected) {
                    log("Kênh thông báo Server đã đóng: " + e.getMessage());
                }
                break;
            }
        }
    }

    public synchronized void disconnect() {
        if (!isConnected) return;
        isConnected = false;

        try {
            if (cmdSocket != null) cmdSocket.close();
        } catch (IOException ignored) {}

        try {
            if (notifySocket != null) notifySocket.close();
        } catch (IOException ignored) {}

        log("Đã ngắt kết nối khỏi máy chủ Sync Server.");
    }

    /**
     * Lấy toàn bộ danh mục tệp tin hiện có trên Server
     */
    public synchronized List<FileMetadata> fetchServerCatalog() throws IOException {
        ensureConnected();
        Message req = new Message(CommandType.LIST_FILES_REQ);
        req.writeTo(cmdOut);

        Message resp = Message.readFrom(cmdIn);
        List<FileMetadata> list = new ArrayList<>();
        if (resp.getCommand() == CommandType.LIST_FILES_RESP) {
            String payload = resp.getPayload();
            if (!payload.isEmpty()) {
                String[] lines = payload.split("\n");
                for (String l : lines) {
                    FileMetadata m = FileMetadata.decode(l);
                    if (m != null) list.add(m);
                }
            }
        }
        return list;
    }

    /**
     * Tải tệp tin lên Server theo khối (Chunking) kèm đối chiếu SHA-256
     */
    public synchronized boolean uploadFile(File file, String relativePath, int clientVersion, BiConsumer<Long, Long> progressCallback) throws IOException {
        ensureConnected();
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("Tệp tin cục bộ không tồn tại: " + file.getAbsolutePath());
        }

        String sha256 = FileUtil.calculateSHA256(file);
        FileMetadata meta = new FileMetadata(
                relativePath,
                file.length(),
                file.lastModified(),
                sha256,
                clientVersion,
                false,
                clientId
        );

        // 1. Gửi yêu cầu UPLOAD_REQ
        Message req = new Message(CommandType.UPLOAD_REQ, "OK", "", meta.encode());
        req.writeTo(cmdOut);

        // 2. Chờ phản hồi ACK hoặc CONFLICT
        Message resp = Message.readFrom(cmdIn);
        if (resp.getCommand() == CommandType.CONFLICT_DETECTED) {
            log(String.format("[CONFLICT] Server thông báo đụng độ phiên bản tệp [%s]", relativePath));
            return false;
        }

        if (resp.getCommand() != CommandType.UPLOAD_ACK) {
            log("Server từ chối tải lên: " + resp.getMessage());
            return false;
        }

        // 3. Truyền dữ liệu nhị phân từng khối (64 KB / Chunk)
        long totalBytes = file.length();
        long sentBytes = 0;
        try (InputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[Constants.CHUNK_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                byte[] chunk;
                if (read == buffer.length) {
                    chunk = buffer;
                } else {
                    chunk = new byte[read];
                    System.arraycopy(buffer, 0, chunk, 0, read);
                }
                Message chunkMsg = new Message(CommandType.UPLOAD_CHUNK, "OK", "", "", chunk);
                chunkMsg.writeTo(cmdOut);

                sentBytes += read;
                if (progressCallback != null) {
                    progressCallback.accept(sentBytes, totalBytes);
                }
            }
        }

        // 4. Thông báo hoàn tất
        Message completeMsg = new Message(CommandType.UPLOAD_COMPLETE);
        completeMsg.writeTo(cmdOut);

        // 5. Chờ xác nhận thành công từ Server sau khi kiểm tra SHA-256
        Message finalResp = Message.readFrom(cmdIn);
        if (finalResp.isSuccess()) {
            log(String.format("[UPLOAD] Đã tải lên tệp [%s] (%s) thành công", relativePath, FileUtil.formatFileSize(totalBytes)));
            return true;
        } else {
            log("[UPLOAD] Lỗi tải lên: " + finalResp.getMessage());
            return false;
        }
    }

    /**
     * Tải tệp từ Server về máy cục bộ theo khối nhị phân
     */
    public synchronized boolean downloadFile(String relativePath, File targetFile, BiConsumer<Long, Long> progressCallback) throws IOException {
        ensureConnected();
        Message req = new Message(CommandType.DOWNLOAD_REQ, "OK", "", relativePath);
        req.writeTo(cmdOut);

        Message ack = Message.readFrom(cmdIn);
        if (ack.getCommand() != CommandType.DOWNLOAD_ACK) {
            log("[DOWNLOAD] Lỗi từ chối tải về: " + ack.getMessage());
            return false;
        }

        FileMetadata serverMeta = FileMetadata.decode(ack.getPayload());
        long totalBytes = (serverMeta != null) ? serverMeta.getSize() : 0;
        long receivedBytes = 0;

        FileUtil.ensureParentDirectory(targetFile);
        File tempFile = new File(targetFile.getParentFile(), targetFile.getName() + ".part.tmp");

        try (FileOutputStream fos = new FileOutputStream(tempFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            while (true) {
                Message chunkMsg = Message.readFrom(cmdIn);
                if (chunkMsg.getCommand() == CommandType.DOWNLOAD_CHUNK) {
                    byte[] data = chunkMsg.getData();
                    if (data != null && data.length > 0) {
                        bos.write(data);
                        receivedBytes += data.length;
                        if (progressCallback != null) {
                            progressCallback.accept(receivedBytes, totalBytes);
                        }
                    }
                } else if (chunkMsg.getCommand() == CommandType.DOWNLOAD_COMPLETE) {
                    bos.flush();
                    break;
                } else {
                    throw new IOException("Gói tin không mong muốn khi tải tệp: " + chunkMsg.getCommand());
                }
            }
        }

        // Xác minh toàn vẹn SHA-256
        if (serverMeta != null && !serverMeta.getSha256().isEmpty()) {
            String actualSha = FileUtil.calculateSHA256(tempFile);
            if (!actualSha.equalsIgnoreCase(serverMeta.getSha256())) {
                tempFile.delete();
                log(String.format("[DOWNLOAD] Sai lệch SHA-256 tệp [%s]", relativePath));
                return false;
            }
        }

        // Thay thế an toàn vào tệp đích
        Files.move(tempFile.toPath(), targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING);

        if (serverMeta != null) {
            targetFile.setLastModified(serverMeta.getLastModified());
        }

        log(String.format("[DOWNLOAD] Đã tải về tệp [%s] (%s) thành công", relativePath, FileUtil.formatFileSize(targetFile.length())));
        return true;
    }

    /**
     * Yêu cầu Server xóa một tệp tin
     */
    public synchronized boolean deleteFileOnServer(String relativePath) throws IOException {
        ensureConnected();
        Message req = new Message(CommandType.DELETE_REQ, "OK", "", relativePath);
        req.writeTo(cmdOut);

        Message resp = Message.readFrom(cmdIn);
        return resp.isSuccess();
    }

    private void ensureConnected() throws IOException {
        if (!isConnected || cmdSocket == null || cmdSocket.isClosed()) {
            throw new IOException("Chưa kết nối đến Sync Server");
        }
    }

    private void log(String msg) {
        String logEntry = String.format("[%s] %s", FileUtil.formatTimestamp(System.currentTimeMillis()), msg);
        if (logListener != null) {
            logListener.accept(logEntry);
        } else {
            System.out.println(logEntry);
        }
    }
}
