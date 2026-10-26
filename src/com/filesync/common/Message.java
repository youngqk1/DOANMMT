package com.filesync.common;

import java.io.*;

/**
 * Lớp đóng gói khung thông điệp giao thức (Protocol Message Frame)
 * Hỗ trợ truyền siêu dữ liệu dạng chuỗi và khối dữ liệu nhị phân tốc độ cao qua TCP Socket
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class Message {
    private CommandType command;
    private String status;        // "OK", "ERROR", "PENDING"
    private String message;       // Mô tả chi tiết hoặc thông báo người dùng
    private String payload;       // Dữ liệu văn bản (Danh sách metadata, tham số...)
    private byte[] data;          // Khối nhị phân đi kèm (chunk tệp tin)

    public Message(CommandType command) {
        this(command, "OK", "", "", null);
    }

    public Message(CommandType command, String status, String message) {
        this(command, status, message, "", null);
    }

    public Message(CommandType command, String status, String message, String payload) {
        this(command, status, message, payload, null);
    }

    public Message(CommandType command, String status, String message, String payload, byte[] data) {
        this.command = command;
        this.status = status != null ? status : "OK";
        this.message = message != null ? message : "";
        this.payload = payload != null ? payload : "";
        this.data = data;
    }

    // Getters và Setters
    public CommandType getCommand() { return command; }
    public void setCommand(CommandType command) { this.command = command; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    public boolean isSuccess() {
        return "OK".equalsIgnoreCase(status);
    }

    /**
     * Gửi gói tin có cấu trúc qua luồng ra Socket (Framed Network Stream)
     */
    public synchronized void writeTo(OutputStream out) throws IOException {
        DataOutputStream dos = (out instanceof DataOutputStream) ? (DataOutputStream) out : new DataOutputStream(out);

        byte[] payloadBytes = payload != null ? payload.getBytes(Constants.DEFAULT_CHARSET) : new byte[0];
        byte[] msgBytes = message != null ? message.getBytes(Constants.DEFAULT_CHARSET) : new byte[0];
        int dataLen = data != null ? data.length : 0;

        // Header format: CMD_ORDINAL (4B) + STATUS (UTF) + MSG_LEN (4B) + PAYLOAD_LEN (4B) + DATA_LEN (4B)
        dos.writeInt(command.ordinal());
        dos.writeUTF(status);
        dos.writeInt(msgBytes.length);
        if (msgBytes.length > 0) {
            dos.write(msgBytes);
        }
        dos.writeInt(payloadBytes.length);
        if (payloadBytes.length > 0) {
            dos.write(payloadBytes);
        }
        dos.writeInt(dataLen);
        if (dataLen > 0) {
            dos.write(data);
        }
        dos.flush();
    }

    /**
     * Đọc gói tin có cấu trúc từ luồng vào Socket
     */
    public static Message readFrom(InputStream in) throws IOException {
        DataInputStream dis = (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in);

        int cmdOrdinal = dis.readInt();
        CommandType[] values = CommandType.values();
        if (cmdOrdinal < 0 || cmdOrdinal >= values.length) {
            throw new IOException("Mã lệnh giao thức không hợp lệ: " + cmdOrdinal);
        }
        CommandType command = values[cmdOrdinal];

        String status = dis.readUTF();

        int msgLen = dis.readInt();
        String message = "";
        if (msgLen > 0) {
            byte[] msgBytes = new byte[msgLen];
            dis.readFully(msgBytes);
            message = new String(msgBytes, Constants.DEFAULT_CHARSET);
        }

        int payloadLen = dis.readInt();
        String payload = "";
        if (payloadLen > 0) {
            byte[] payloadBytes = new byte[payloadLen];
            dis.readFully(payloadBytes);
            payload = new String(payloadBytes, Constants.DEFAULT_CHARSET);
        }

        int dataLen = dis.readInt();
        byte[] data = null;
        if (dataLen > 0) {
            data = new byte[dataLen];
            dis.readFully(data);
        }

        return new Message(command, status, message, payload, data);
    }

    @Override
    public String toString() {
        return "Message{" +
                "cmd=" + command +
                ", status='" + status + '\'' +
                ", msg='" + message + '\'' +
                ", dataLen=" + (data != null ? data.length : 0) +
                '}';
    }
}
