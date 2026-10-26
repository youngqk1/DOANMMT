package com.filesync.client;

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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giao diện người dùng đồ họa Client (Client GUI Dashboard)
 * Trình diễn trực quan trạng thái đồng bộ, thanh tiến trình, điều khiển đồng bộ 2 chiều
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class ClientGUI extends JFrame {
    private final JTextField txtHost;
    private final JTextField txtPort;
    private final JTextField txtClientId;
    private final JTextField txtSyncFolder;
    private final JButton btnBrowseFolder;
    private final JButton btnConnect;
    private final JButton btnDisconnect;

    private final JButton btnSyncNow;
    private final JCheckBox chkAutoSync;
    private final JButton btnOpenLocalFolder;

    private final JProgressBar progressBar;
    private final JLabel lblProgressText;

    private final DefaultTableModel tableModel;
    private final JTextArea txtLog;
    private final Map<String, SyncEngine.SyncStatus> statusMap = new ConcurrentHashMap<>();

    private SyncClient client;
    private SyncEngine engine;
    private File currentSyncDir;

    public ClientGUI(String defaultHost, int defaultPort, String defaultClientId, File defaultFolder) {
        this.currentSyncDir = defaultFolder;

        setTitle("FileSync Client - " + defaultClientId + " [Trần Nguyễn Hoàng Quốc Khánh]");
        setSize(960, 680);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // 1. THANH CẤU HÌNH KẾT NỐI (NORTH PANEL)
        JPanel northPanel = new JPanel(new BorderLayout(8, 8));
        northPanel.setBorder(new EmptyBorder(12, 16, 10, 16));
        northPanel.setBackground(new Color(245, 247, 250));

        JPanel connRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        connRow1.setOpaque(false);

        connRow1.add(new JLabel("Máy chủ:"));
        txtHost = new JTextField(defaultHost, 10);
        connRow1.add(txtHost);

        connRow1.add(new JLabel("Cổng:"));
        txtPort = new JTextField(String.valueOf(defaultPort), 5);
        connRow1.add(txtPort);

        connRow1.add(new JLabel("Tên định danh:"));
        txtClientId = new JTextField(defaultClientId, 12);
        connRow1.add(txtClientId);

        btnConnect = new JButton("Kết nối Server");
        btnConnect.setBackground(new Color(34, 197, 94));
        btnConnect.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnDisconnect = new JButton("Ngắt kết nối");
        btnDisconnect.setBackground(new Color(239, 68, 68));
        btnDisconnect.setEnabled(false);

        connRow1.add(btnConnect);
        connRow1.add(btnDisconnect);

        JPanel connRow2 = new JPanel(new BorderLayout(8, 4));
        connRow2.setOpaque(false);

        JPanel folderPanel = new JPanel(new BorderLayout(5, 0));
        folderPanel.setOpaque(false);
        folderPanel.add(new JLabel("Thư mục đồng bộ: "), BorderLayout.WEST);
        txtSyncFolder = new JTextField(currentSyncDir.getAbsolutePath());
        txtSyncFolder.setEditable(false);
        btnBrowseFolder = new JButton("Chọn thư mục...");
        folderPanel.add(txtSyncFolder, BorderLayout.CENTER);
        folderPanel.add(btnBrowseFolder, BorderLayout.EAST);

        connRow2.add(folderPanel, BorderLayout.CENTER);

        northPanel.add(connRow1, BorderLayout.NORTH);
        northPanel.add(connRow2, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);

        // 2. THANH ĐIỀU KHIỂN ĐỒNG BỘ VÀ TIẾN TRÌNH
        JPanel toolbarPanel = new JPanel(new BorderLayout(10, 8));
        toolbarPanel.setBorder(new EmptyBorder(6, 16, 8, 16));

        JPanel syncActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        btnSyncNow = new JButton("🔄 Đồng bộ ngay");
        btnSyncNow.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSyncNow.setEnabled(false);

        chkAutoSync = new JCheckBox("Tự động đồng bộ (Real-time Watcher)", true);
        chkAutoSync.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        btnOpenLocalFolder = new JButton("Mở thư mục máy");

        syncActions.add(btnSyncNow);
        syncActions.add(chkAutoSync);
        syncActions.add(btnOpenLocalFolder);

        JPanel progressPanel = new JPanel(new BorderLayout(6, 4));
        lblProgressText = new JLabel("Sẵn sàng");
        lblProgressText.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(280, 20));

        progressPanel.add(lblProgressText, BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        toolbarPanel.add(syncActions, BorderLayout.WEST);
        toolbarPanel.add(progressPanel, BorderLayout.EAST);

        // 3. VÙNG BẢNG DANH MỤC TỆP TIN & NHẬT KÝ
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab A: Danh sách tệp
        String[] columns = {"STT", "Tên tệp tin", "Đường dẫn", "Kích thước", "Trạng thái đồng bộ", "Thời gian sửa đổi"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tblFiles = new JTable(tableModel);
        tblFiles.setRowHeight(26);

        // Render màu sắc trạng thái đồng bộ
        tblFiles.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
                String text = (value != null) ? value.toString() : "";
                if (text.contains("ĐÃ ĐỒNG BỘ") || text.contains("SYNCED")) {
                    lbl.setForeground(new Color(22, 101, 52));
                } else if (text.contains("TẢI LÊN") || text.contains("UPLOADING")) {
                    lbl.setForeground(new Color(29, 78, 216));
                } else if (text.contains("TẢI VỀ") || text.contains("DOWNLOADING")) {
                    lbl.setForeground(new Color(13, 148, 136));
                } else if (text.contains("XUNG ĐỘT") || text.contains("CONFLICT")) {
                    lbl.setForeground(new Color(185, 28, 28));
                } else {
                    lbl.setForeground(new Color(75, 85, 99));
                }
                return lbl;
            }
        });

        tabbedPane.addTab("  Danh Sách Tệp Tin & Trạng Thái  ", new JScrollPane(tblFiles));

        // Tab B: Nhật ký sự kiện
        JPanel logPanel = new JPanel(new BorderLayout(5, 5));
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(15, 23, 42));
        txtLog.setForeground(new Color(226, 232, 240));
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setMargin(new Insets(8, 8, 8, 8));

        logPanel.add(new JScrollPane(txtLog), BorderLayout.CENTER);

        JPanel logBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        JButton btnClear = new JButton("Xóa log");
        btnClear.addActionListener(e -> txtLog.setText(""));
        logBar.add(btnClear);
        logPanel.add(logBar, BorderLayout.SOUTH);

        tabbedPane.addTab("  Nhật Ký Đồng Bộ (Activity Log)  ", logPanel);

        // Bố cục thân cửa sổ
        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.add(toolbarPanel, BorderLayout.NORTH);
        centerContainer.add(tabbedPane, BorderLayout.CENTER);
        add(centerContainer, BorderLayout.CENTER);

        // 4. LẮNG NGHE SỰ KIỆN NÚT BẤM
        btnConnect.addActionListener(e -> connectToServer());
        btnDisconnect.addActionListener(e -> disconnectFromServer());
        btnBrowseFolder.addActionListener(e -> chooseLocalFolder());
        btnOpenLocalFolder.addActionListener(e -> openLocalFolder());
        btnSyncNow.addActionListener(e -> triggerManualSync());
        chkAutoSync.addActionListener(e -> {
            if (engine != null) {
                engine.setAutoSyncEnabled(chkAutoSync.isSelected());
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectFromServer();
                dispose();
                System.exit(0);
            }
        });

        // Tải danh sách tệp ban đầu
        refreshFileTable();
    }

    private void chooseLocalFolder() {
        JFileChooser chooser = new JFileChooser(currentSyncDir);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            currentSyncDir = chooser.getSelectedFile();
            txtSyncFolder.setText(currentSyncDir.getAbsolutePath());
            refreshFileTable();
        }
    }

    private void openLocalFolder() {
        try {
            if (Desktop.isDesktopSupported() && currentSyncDir.exists()) {
                Desktop.getDesktop().open(currentSyncDir);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không thể mở thư mục: " + e.getMessage());
        }
    }

    private void connectToServer() {
        String host = txtHost.getText().trim();
        int port;
        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Cổng không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String inputId = txtClientId.getText().trim();
        final String id = inputId.isEmpty() ? "Client-" + (System.currentTimeMillis() % 1000) : inputId;

        btnConnect.setEnabled(false);
        txtHost.setEnabled(false);
        txtPort.setEnabled(false);
        txtClientId.setEnabled(false);
        btnBrowseFolder.setEnabled(false);

        new Thread(() -> {
            try {
                client = new SyncClient(host, port, id);
                client.setLogListener(this::appendLog);

                client.connect();

                engine = new SyncEngine(currentSyncDir, client);
                engine.setLogListener(this::appendLog);
                engine.setAutoSyncEnabled(chkAutoSync.isSelected());
                engine.setFileStatusListener((path, status) -> {
                    statusMap.put(path, status);
                    SwingUtilities.invokeLater(this::refreshFileTable);
                });
                engine.setProgressListener((sent, total) -> SwingUtilities.invokeLater(() -> {
                    if (total > 0) {
                        int percent = (int) ((sent * 100) / total);
                        progressBar.setValue(percent);
                        lblProgressText.setText(String.format("%s / %s (%d%%)",
                                FileUtil.formatFileSize(sent), FileUtil.formatFileSize(total), percent));
                    }
                }));
                engine.setCatalogChangeListener(() -> SwingUtilities.invokeLater(this::refreshFileTable));
                engine.start();

                SwingUtilities.invokeLater(() -> {
                    btnDisconnect.setEnabled(true);
                    btnSyncNow.setEnabled(true);
                    appendLog("Kết nối thành công đến máy chủ! Tiến hành đồng bộ khởi tạo...");
                });

                // Tự động đồng bộ lần đầu ngay khi vừa kết nối
                engine.syncAll();
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Không thể kết nối đến Server: " + e.getMessage(), "Lỗi kết nối", JOptionPane.ERROR_MESSAGE);
                    btnConnect.setEnabled(true);
                    txtHost.setEnabled(true);
                    txtPort.setEnabled(true);
                    txtClientId.setEnabled(true);
                    btnBrowseFolder.setEnabled(true);
                });
            }
        }).start();
    }

    private void disconnectFromServer() {
        if (engine != null) {
            engine.stop();
        }
        if (client != null) {
            client.disconnect();
        }
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
        btnSyncNow.setEnabled(false);
        txtHost.setEnabled(true);
        txtPort.setEnabled(true);
        txtClientId.setEnabled(true);
        btnBrowseFolder.setEnabled(true);
        progressBar.setValue(0);
        lblProgressText.setText("Đã ngắt kết nối");
    }

    private void triggerManualSync() {
        if (engine == null) return;
        btnSyncNow.setEnabled(false);
        lblProgressText.setText("Đang đồng bộ...");
        progressBar.setValue(0);

        new Thread(() -> {
            try {
                engine.syncAll();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    btnSyncNow.setEnabled(true);
                    lblProgressText.setText("Đồng bộ hoàn tất");
                    progressBar.setValue(100);
                });
            }
        }).start();
    }

    private void refreshFileTable() {
        tableModel.setRowCount(0);
        try {
            List<FileMetadata> files = FileUtil.scanDirectory(currentSyncDir);
            int idx = 1;
            for (FileMetadata f : files) {
                SyncEngine.SyncStatus status = statusMap.getOrDefault(f.getRelativePath(), SyncEngine.SyncStatus.SYNCED);
                String statusStr;
                switch (status) {
                    case UPLOADING: statusStr = "⬆️ ĐANG TẢI LÊN"; break;
                    case DOWNLOADING: statusStr = "⬇️ ĐANG TẢI VỀ"; break;
                    case CONFLICT: statusStr = "⚠️ XUNG ĐỘT (ĐÃ TÁCH)"; break;
                    case ERROR: statusStr = "❌ LỖI"; break;
                    default: statusStr = "✅ ĐÃ ĐỒNG BỘ"; break;
                }

                tableModel.addRow(new Object[]{
                        idx++,
                        f.getFileName(),
                        f.getRelativePath(),
                        FileUtil.formatFileSize(f.getSize()),
                        statusStr,
                        FileUtil.formatTimestamp(f.getLastModified())
                });
            }
        } catch (Exception ignored) {}
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            txtLog.append(msg + "\n");
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
}
