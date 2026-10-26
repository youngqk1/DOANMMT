package com.filesync.server;

import com.filesync.common.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

/**
 * Luồng phục vụ mạng độc lập cho từng Client kết nối đến Server
 * Xử lý giao thức đa luồng: Tải lên, Tải xuống, Đồng bộ danh mục, Quản lý phiên bản
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final SyncServer server;
    private final ServerStorageManager storageManager;

    private DataInputStream in;
    private DataOutputStream out;
    private String clientId = "Unknown";
    private String clientIp = "";
    private long connectedTime;
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket, SyncServer server, ServerStorageManager storageManager) {
        this.socket = socket;
        this.server = server;
        this.storageManager = storageManager;
        this.connectedTime = System.currentTimeMillis();
        if (socket.getInetAddress() != null) {
            this.clientIp = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
        }
    }

    public String getClientId() { return clientId; }
    public String getClientIp() { return clientIp; }
    public long getConnectedTime() { return connectedTime; }

    public boolean isNotifyChannel() {
        return clientId != null && clientId.endsWith(":NOTIFY");
    }

    public String getBaseClientId() {
        if (clientId == null) return "";
        int idx = clientId.indexOf(':');
        return idx >= 0 ? clientId.substring(0, idx) : clientId;
    }

    @Override
    public void run() {
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            server.log(String.format("Client kết nối mới từ [%s]", clientIp));

            while (isRunning) {
                Message request;
                try {
                    request = Message.readFrom(in);
                } catch (EOFException e) {
                    break; // Client đã ngắt kết nối
                } catch (IOException e) {
                    if (isRunning) {
                        server.log("Lỗi đọc yêu cầu từ " + clientId + ": " + e.getMessage());
                    }
                    break;
                }

                if (request == null) break;
                handleRequest(request);
            }
        } catch (Exception e) {
            server.log("Lỗi socket của client " + clientId + ": " + e.getMessage());
        } finally {
            close();
        }
    }

    private void handleRequest(Message req) throws IOException {
        switch (req.getCommand()) {
            case HANDSHAKE:
                this.clientId = req.getPayload().isEmpty() ? "Client-" + socket.getPort() : req.getPayload();
                server.log(String.format("Handshake thành công: [%s] (%s)", clientId, clientIp));
                sendMessage(new Message(CommandType.HANDSHAKE_ACK, "OK", "Chào mừng " + clientId + " đến với Sync Server"));
                server.onClientStateChanged();
                break;

            case LIST_FILES_REQ:
                handleListFiles();
                break;

            case UPLOAD_REQ:
                handleUpload(req);
                break;

            case DOWNLOAD_REQ:
                handleDownload(req);
                break;

            case DELETE_REQ:
                handleDelete(req);
                break;

            case GET_VERSIONS_REQ:
                handleGetVersions(req);
                break;

            case ROLLBACK_REQ:
                handleRollback(req);
                break;

            default:
                sendMessage(new Message(CommandType.ERROR, "ERROR", "Lệnh không hỗ trợ: " + req.getCommand()));
                break;
        }
    }

    private void handleListFiles() throws IOException {
        List<FileMetadata> files = storageManager.getAllFiles();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.size(); i++) {
            sb.append(files.get(i).encode());
            if (i < files.size() - 1) {
                sb.append("\n");
            }
        }
        sendMessage(new Message(CommandType.LIST_FILES_RESP, "OK", "Danh mục gồm " + files.size() + " tệp tin", sb.toString()));
    }

    private void handleUpload(Message req) throws IOException {
        FileMetadata clientMeta = FileMetadata.decode(req.getPayload());
        if (clientMeta == null) {
            sendMessage(new Message(CommandType.ERROR, "ERROR", "Dữ liệu metadata tải lên không hợp lệ"));
            return;
        }

        // Kiểm tra khả năng đụng độ phiên bản
        if (storageManager.isConflict(clientMeta)) {
            FileMetadata serverMeta = storageManager.getMetadata(clientMeta.getRelativePath());
            server.log(String.format("[CONFLICT] Phát hiện xung đột phiên bản tệp [%s] từ Client [%s]",
                    clientMeta.getRelativePath(), clientId));
            sendMessage(new Message(CommandType.CONFLICT_DETECTED, "CONFLICT",
                    "Phát hiện xung đột phiên bản trên Server", serverMeta != null ? serverMeta.encode() : ""));
            return;
        }

        // Chấp thuận bắt đầu nhận khối tệp
        sendMessage(new Message(CommandType.UPLOAD_ACK, "OK", "Server sẵn sàng nhận dữ liệu khối"));

        // Tạo tệp tạm trong thư mục lưu trữ để đảm bảo cùng phân vùng đĩa
        File tempFile = new File(storageManager.getRootDir(), ".up_" + System.nanoTime() + ".part");
        try {
            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                while (true) {
                    Message chunkMsg = Message.readFrom(in);
                    if (chunkMsg.getCommand() == CommandType.UPLOAD_CHUNK) {
                        byte[] data = chunkMsg.getData();
                        if (data != null && data.length > 0) {
                            bos.write(data);
                        }
                    } else if (chunkMsg.getCommand() == CommandType.UPLOAD_COMPLETE) {
                        bos.flush();
                        break;
                    } else {
                        throw new IOException("Mong đợi UPLOAD_CHUNK hoặc UPLOAD_COMPLETE nhưng nhận: " + chunkMsg.getCommand());
                    }
                }
            } // Đóng luồng ghi tệp tạm trước khi tính hash và di chuyển

            // Kiểm tra tính toàn vẹn SHA-256 sau khi nhận xong
            String actualSha = FileUtil.calculateSHA256(tempFile);
            if (!actualSha.equalsIgnoreCase(clientMeta.getSha256())) {
                tempFile.delete();
                sendMessage(new Message(CommandType.ERROR, "ERROR",
                        "Sai lệch mã băm SHA-256. Dự kiến: " + clientMeta.getSha256() + ", Thực tế: " + actualSha));
                return;
            }

            // Cam kết lưu trữ tệp vào thư mục chính thức và ghi nhận lịch sử phiên bản
            FileMetadata savedMeta = storageManager.commitUpload(
                    clientMeta.getRelativePath(),
                    tempFile,
                    clientId,
                    clientMeta.getLastModified()
            );

            server.log(String.format("[UPLOAD] Hoàn tất nhận tệp [%s] (%s, v%d) từ [%s]",
                    savedMeta.getRelativePath(), FileUtil.formatFileSize(savedMeta.getSize()), savedMeta.getVersion(), clientId));

            sendMessage(new Message(CommandType.SUCCESS, "OK", "Tải lên thành công", savedMeta.encode()));

            // Phát tán thông báo cập nhật tệp tin đến TẤT CẢ các Client khác trong mạng
            server.broadcastNotification(new Message(CommandType.NOTIFY_CHANGE, "OK", "Tệp tin đã thay đổi", savedMeta.encode()), this);
            server.onFileCatalogChanged();
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private void handleDownload(Message req) throws IOException {
        String relativePath = req.getPayload();
        FileMetadata meta = storageManager.getMetadata(relativePath);
        if (meta == null) {
            sendMessage(new Message(CommandType.ERROR, "ERROR", "Không tìm thấy tệp trên máy chủ: " + relativePath));
            return;
        }

        File targetFile = FileUtil.resolveSafe(storageManager.getRootDir(), relativePath);
        if (!targetFile.exists() || !targetFile.isFile()) {
            sendMessage(new Message(CommandType.ERROR, "ERROR", "Tệp vật lý không tồn tại: " + relativePath));
            return;
        }

        // Báo cho Client biết chuẩn bị nhận dữ liệu tệp
        sendMessage(new Message(CommandType.DOWNLOAD_ACK, "OK", "Bắt đầu truyền tệp", meta.encode()));

        // Đọc tệp và gửi từng khối (Chunking) 64KB
        try (InputStream fis = new BufferedInputStream(new FileInputStream(targetFile))) {
            byte[] buffer = new byte[Constants.CHUNK_SIZE];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                byte[] chunkData;
                if (read == buffer.length) {
                    chunkData = buffer;
                } else {
                    chunkData = new byte[read];
                    System.arraycopy(buffer, 0, chunkData, 0, read);
                }
                sendMessage(new Message(CommandType.DOWNLOAD_CHUNK, "OK", "", "", chunkData));
            }
        }

        // Thông báo kết thúc tệp
        sendMessage(new Message(CommandType.DOWNLOAD_COMPLETE, "OK", "Đã truyền xong toàn bộ tệp"));
        server.log(String.format("[DOWNLOAD] Đã gửi tệp [%s] cho Client [%s]", relativePath, clientId));
    }

    private void handleDelete(Message req) throws IOException {
        String relativePath = req.getPayload();
        boolean deleted = storageManager.deleteFile(relativePath, clientId);
        if (deleted) {
            server.log(String.format("[DELETE] Client [%s] đã xóa tệp [%s]", clientId, relativePath));
            sendMessage(new Message(CommandType.DELETE_RESP, "OK", "Đã xóa tệp thành công"));

            // Tạo tombstone metadata để broadcast lệnh xóa
            FileMetadata tombstone = new FileMetadata(relativePath, 0, System.currentTimeMillis(), "", 0, true, clientId);
            server.broadcastNotification(new Message(CommandType.NOTIFY_CHANGE, "OK", "Tệp đã bị xóa", tombstone.encode()), this);
            server.onFileCatalogChanged();
        } else {
            sendMessage(new Message(CommandType.DELETE_RESP, "ERROR", "Không thể xóa tệp hoặc tệp không tồn tại"));
        }
    }

    private void handleGetVersions(Message req) throws IOException {
        String relativePath = req.getPayload();
        List<String> versions = storageManager.getVersionHistory(relativePath);
        String joined = String.join("\n", versions);
        sendMessage(new Message(CommandType.GET_VERSIONS_RESP, "OK", "Danh sách phiên bản", joined));
    }

    private void handleRollback(Message req) throws IOException {
        String[] parts = req.getPayload().split("\t");
        if (parts.length < 2) {
            sendMessage(new Message(CommandType.ERROR, "ERROR", "Cú pháp rollback không hợp lệ"));
            return;
        }
        String relPath = parts[0];
        String verName = parts[1];
        try {
            FileMetadata rolled = storageManager.rollbackVersion(relPath, verName, clientId);
            server.log(String.format("[ROLLBACK] Client [%s] khôi phục tệp [%s] về [%s]", clientId, relPath, verName));
            sendMessage(new Message(CommandType.ROLLBACK_RESP, "OK", "Khôi phục thành công", rolled.encode()));

            server.broadcastNotification(new Message(CommandType.NOTIFY_CHANGE, "OK", "Khôi phục phiên bản", rolled.encode()), this);
            server.onFileCatalogChanged();
        } catch (Exception e) {
            sendMessage(new Message(CommandType.ERROR, "ERROR", "Lỗi khôi phục phiên bản: " + e.getMessage()));
        }
    }

    public synchronized void sendMessage(Message message) throws IOException {
        message.writeTo(out);
    }

    public void close() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
        server.removeClient(this);
        server.log(String.format("Client [%s] (%s) đã ngắt kết nối", clientId, clientIp));
        server.onClientStateChanged();
    }
}
