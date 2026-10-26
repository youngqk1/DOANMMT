package com.filesync.server;

import com.filesync.common.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

/**
 * Giao diện điều khiển và giám sát máy chủ đồ họa (Server GUI Dashboard)
 * Hiển thị số lượng Client trực tuyến, danh mục tệp tin, phiên bản và nhật ký hoạt động
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class ServerGUI extends JFrame {
    private final JTextField txtPort;
    private final JButton btnStart;
    private final JButton btnStop;
    private final JButton btnOpenDir;
    private final JLabel lblStatus;
    private final JLabel lblClientCount;
    private final JLabel lblFileCount;

    private final DefaultTableModel clientTableModel;
    private final DefaultTableModel fileTableModel;
    private final JTextArea txtLog;

    private SyncServer server;
    private final File storageDir;

    public ServerGUI(int defaultPort, File storageDirectory) {
        this.storageDir = storageDirectory;

        setTitle("FileSync Server Dashboard - Trần Nguyễn Hoàng Quốc Khánh (2280601477)");
        setSize(950, 680);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // Thiết lập giao diện hiện đại
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // 1. THANH TIÊU ĐỀ & ĐIỀU KHIỂN TRÊN CÙNG
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new EmptyBorder(12, 16, 12, 16));
        topPanel.setBackground(new Color(245, 247, 250));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setOpaque(false);

        JLabel lblTitle = new JLabel("MÁY CHỦ ĐỒNG BỘ TỆP TIN");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(new Color(30, 41, 59));

        lblStatus = new JLabel("  ĐÃ DỪNG  ");
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(239, 68, 68));
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        headerLeft.add(lblTitle);
        headerLeft.add(lblStatus);

        JPanel headerControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerControls.setOpaque(false);

        headerControls.add(new JLabel("Cổng (Port):"));
        txtPort = new JTextField(String.valueOf(defaultPort), 5);
        headerControls.add(txtPort);

        btnStart = new JButton("Khởi động Server");
        btnStart.setBackground(new Color(34, 197, 94));
        btnStart.setForeground(Color.BLACK);
        btnStart.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnStop = new JButton("Dừng Server");
        btnStop.setBackground(new Color(239, 68, 68));
        btnStop.setForeground(Color.BLACK);
        btnStop.setEnabled(false);

        btnOpenDir = new JButton("Mở thư mục kho");

        headerControls.add(btnStart);
        headerControls.add(btnStop);
        headerControls.add(btnOpenDir);

        topPanel.add(headerLeft, BorderLayout.WEST);
        topPanel.add(headerControls, BorderLayout.EAST);

        // 2. THANH THỐNG KÊ NHANH (SUMMARY STATS)
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        statsPanel.setBorder(new EmptyBorder(0, 16, 10, 16));
        statsPanel.setBackground(new Color(245, 247, 250));

        lblClientCount = createStatCard("Client đang kết nối", "0", new Color(59, 130, 246));
        lblFileCount = createStatCard("Tệp tin quản lý", "0", new Color(16, 185, 129));
        JLabel lblStoragePath = createStatCard("Đường dẫn lưu trữ", storageDir.getName(), new Color(139, 92, 246));

        statsPanel.add(lblClientCount.getParent());
        statsPanel.add(lblFileCount.getParent());
        statsPanel.add(lblStoragePath.getParent());

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(statsPanel, BorderLayout.SOUTH);
        add(northContainer, BorderLayout.NORTH);

        // 3. VÙNG TAB CHÍNH
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab A: Danh sách Client
        String[] clientCols = {"STT", "Client ID", "Địa chỉ IP / Port", "Thời điểm kết nối"};
        clientTableModel = new DefaultTableModel(clientCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblClients = new JTable(clientTableModel);
        tblClients.setRowHeight(24);
        tabbedPane.addTab("  Clients Trực Tuyến  ", new JScrollPane(tblClients));

        // Tab B: Danh mục Tệp tin
        String[] fileCols = {"STT", "Tên tệp", "Đường dẫn tương đối", "Kích thước", "Phiên bản", "Mã SHA-256", "Cập nhật bởi", "Thời gian"};
        fileTableModel = new DefaultTableModel(fileCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblFiles = new JTable(fileTableModel);
        tblFiles.setRowHeight(24);

        JPanel fileTabPanel = new JPanel(new BorderLayout(5, 5));
        fileTabPanel.add(new JScrollPane(tblFiles), BorderLayout.CENTER);

        JPanel fileToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton btnRefreshFiles = new JButton("Làm mới danh mục");
        JButton btnViewVersions = new JButton("Xem lịch sử phiên bản");
        fileToolbar.add(btnRefreshFiles);
        fileToolbar.add(btnViewVersions);
        fileTabPanel.add(fileToolbar, BorderLayout.SOUTH);

        tabbedPane.addTab("  Kho Tệp Tin & Phiên Bản  ", fileTabPanel);

        // Tab C: Nhật ký sự kiện (Logs)
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(15, 23, 42));
        txtLog.setForeground(new Color(226, 232, 240));
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane logScroll = new JScrollPane(txtLog);
        logPanel.add(logScroll, BorderLayout.CENTER);

        JPanel logToolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton btnClearLog = new JButton("Xóa nhật ký");
        btnClearLog.addActionListener(e -> txtLog.setText(""));
        logToolbar.add(btnClearLog);
        logPanel.add(logToolbar, BorderLayout.SOUTH);

        tabbedPane.addTab("  Nhật Ký Sự Kiện Real-time  ", logPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // 4. SỰ KIỆN NÚT BẤM VÀ ĐIỀU KHIỂN
        btnStart.addActionListener(e -> startServer());
        btnStop.addActionListener(e -> stopServer());
        btnOpenDir.addActionListener(e -> openStorageDirectory());
        btnRefreshFiles.addActionListener(e -> updateFileCatalogTable());
        btnViewVersions.addActionListener(e -> viewSelectedFileVersions(tblFiles));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (server != null && server.isRunning()) {
                    int ans = JOptionPane.showConfirmDialog(ServerGUI.this,
                            "Máy chủ đang hoạt động. Bạn có chắc muốn dừng server và thoát?",
                            "Xác nhận thoát", JOptionPane.YES_NO_OPTION);
                    if (ans == JOptionPane.YES_OPTION) {
                        stopServer();
                        dispose();
                        System.exit(0);
                    }
                } else {
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    private JLabel createStatCard(String title, String val, Color color) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        card.setBackground(Color.WHITE);

        JLabel lblT = new JLabel(title);
        lblT.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblT.setForeground(new Color(100, 116, 139));

        JLabel lblV = new JLabel(val);
        lblV.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblV.setForeground(color);

        card.add(lblT, BorderLayout.NORTH);
        card.add(lblV, BorderLayout.CENTER);
        return lblV;
    }

    private void startServer() {
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng mạng không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        server = new SyncServer(port, storageDir);
        server.setLogListener(msg -> SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        }));
        server.setClientStateListener(() -> SwingUtilities.invokeLater(this::updateClientTable));
        server.setFileCatalogListener(() -> SwingUtilities.invokeLater(this::updateFileCatalogTable));

        try {
            server.start();
            lblStatus.setText("  ĐANG CHẠY  ");
            lblStatus.setBackground(new Color(34, 197, 94));
            btnStart.setEnabled(false);
            btnStop.setEnabled(true);
            txtPort.setEnabled(false);
            updateFileCatalogTable();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể khởi động Server: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stopServer() {
        if (server != null) {
            server.stop();
            lblStatus.setText("  ĐÃ DỪNG  ");
            lblStatus.setBackground(new Color(239, 68, 68));
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
            txtPort.setEnabled(true);
            updateClientTable();
        }
    }

    private void openStorageDirectory() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(storageDir);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể mở thư mục: " + e.getMessage(), "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateClientTable() {
        clientTableModel.setRowCount(0);
        if (server == null) {
            lblClientCount.setText("0");
            return;
        }
        List<ClientHandler> active = server.getActiveClients();
        lblClientCount.setText(String.valueOf(active.size()));
        int idx = 1;
        for (ClientHandler h : active) {
            clientTableModel.addRow(new Object[]{
                    idx++,
                    h.getClientId(),
                    h.getClientIp(),
                    FileUtil.formatTimestamp(h.getConnectedTime())
            });
        }
    }

    private void updateFileCatalogTable() {
        fileTableModel.setRowCount(0);
        if (server == null) return;

        List<FileMetadata> files = server.getStorageManager().getAllFiles();
        lblFileCount.setText(String.valueOf(files.size()));
        int idx = 1;
        for (FileMetadata f : files) {
            String shortSha = f.getSha256().length() > 12 ? f.getSha256().substring(0, 12) + "..." : f.getSha256();
            fileTableModel.addRow(new Object[]{
                    idx++,
                    f.getFileName(),
                    f.getRelativePath(),
                    FileUtil.formatFileSize(f.getSize()),
                    "v" + f.getVersion(),
                    shortSha,
                    f.getUpdatedBy(),
                    FileUtil.formatTimestamp(f.getLastModified())
            });
        }
    }

    private void viewSelectedFileVersions(JTable tblFiles) {
        int row = tblFiles.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một tệp tin trong danh mục để xem phiên bản!", "Nhắc nhở", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String relPath = (String) fileTableModel.getValueAt(row, 2);
        List<String> versions = server.getStorageManager().getVersionHistory(relPath);

        if (versions.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tệp [" + relPath + "] hiện chỉ có 1 phiên bản gốc (chưa có bản sao lưu trước đó).", "Lịch sử phiên bản", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder("Danh sách phiên bản lịch sử của [" + relPath + "]:\n\n");
        for (String v : versions) {
            sb.append("• ").append(v).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Lịch sử phiên bản tệp tin", JOptionPane.INFORMATION_MESSAGE);
    }
}
