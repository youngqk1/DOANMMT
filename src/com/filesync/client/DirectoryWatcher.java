package com.filesync.client;

import com.filesync.common.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Lớp giám sát thay đổi thư mục cục bộ theo thời gian thực sử dụng Java NIO WatchService
 * Bắt các sự kiện tạo mới (CREATE), chỉnh sửa (MODIFY) và xóa (DELETE) với cơ chế Debounce
 * Đồ án: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
 * Nhóm 14 - Lập trình Mạng Máy Tính
 */
public class DirectoryWatcher implements Runnable {
    public enum EventType { CREATE, MODIFY, DELETE }

    private final Path rootPath;
    private final BiConsumer<EventType, String> changeListener;
    private WatchService watchService;
    private final Map<WatchKey, Path> keyPathMap = new ConcurrentHashMap<>();
    private final Map<String, Long> lastTriggerTime = new ConcurrentHashMap<>();
    private final Set<String> suppressedPaths = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private volatile boolean isRunning = false;
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();

    public DirectoryWatcher(File watchDir, BiConsumer<EventType, String> changeListener) {
        this.rootPath = watchDir.toPath();
        this.changeListener = changeListener;
    }

    public synchronized void start() throws IOException {
        if (isRunning) return;
        this.watchService = FileSystems.getDefault().newWatchService();
        registerAll(rootPath);
        this.isRunning = true;
        Thread thread = new Thread(this, "DirectoryWatcher-" + rootPath.getFileName());
        thread.setDaemon(true);
        thread.start();
    }

    public synchronized void stop() {
        if (!isRunning) return;
        isRunning = false;
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException ignored) {}
        debounceExecutor.shutdownNow();
    }

    /**
     * Bỏ qua sự kiện cho một đường dẫn cụ thể (khi Client đang tải tệp từ Server về)
     */
    public void suppress(String relativePath) {
        suppressedPaths.add(FileMetadata.normalizePath(relativePath));
    }

    public void unsuppress(String relativePath) {
        suppressedPaths.remove(FileMetadata.normalizePath(relativePath));
    }

    /**
     * Đăng ký giám sát đệ quy toàn bộ thư mục con
     */
    private void registerAll(Path start) throws IOException {
        Files.walk(start)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try {
                        String rel = rootPath.relativize(dir).toString().replace('\\', '/');
                        if (rel.startsWith(".") || rel.contains("/.")) return; // Bỏ qua thư mục ẩn
                        WatchKey key = dir.register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY,
                                StandardWatchEventKinds.ENTRY_DELETE);
                        keyPathMap.put(key, dir);
                    } catch (IOException ignored) {}
                });
    }

    @Override
    public void run() {
        while (isRunning) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                break;
            }

            Path dir = keyPathMap.get(key);
            if (dir == null) {
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path name = ev.context();
                Path child = dir.resolve(name);

                String relPath = rootPath.relativize(child).toString().replace('\\', '/');

                // Bỏ qua các tệp ẩn, tệp tạm
                if (relPath.startsWith(".") || relPath.contains("/.") || relPath.endsWith(".tmp") || relPath.endsWith("~")) {
                    continue;
                }

                // Nếu thư mục mới được tạo, đăng ký thêm vào WatchService
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(child)) {
                    try {
                        registerAll(child);
                    } catch (IOException ignored) {}
                }

                EventType type = (kind == StandardWatchEventKinds.ENTRY_CREATE) ? EventType.CREATE
                        : (kind == StandardWatchEventKinds.ENTRY_MODIFY) ? EventType.MODIFY
                        : EventType.DELETE;

                handleDebouncedEvent(type, relPath);
            }

            boolean valid = key.reset();
            if (!valid) {
                keyPathMap.remove(key);
            }
        }
    }

    /**
     * Lọc và gộp các sự kiện phát sinh liên tục trong khoảng thời gian ngắn (Debouncing)
     */
    private void handleDebouncedEvent(EventType type, String relativePath) {
        String normPath = FileMetadata.normalizePath(relativePath);

        // Kiểm tra xem đường dẫn có đang nằm trong danh sách tạm bỏ qua không
        if (suppressedPaths.contains(normPath)) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastTime = lastTriggerTime.get(normPath);

        // Nếu vừa kích hoạt trong vòng WATCH_DEBOUNCE_MS thì bỏ qua để chống lặp
        if (lastTime != null && (now - lastTime < Constants.WATCH_DEBOUNCE_MS)) {
            return;
        }
        lastTriggerTime.put(normPath, now);

        // Đẩy sự kiện qua bộ hẹn giờ ngắn để bảo đảm tệp đã ghi hoàn tất vào đĩa
        debounceExecutor.schedule(() -> {
            try {
                // Kiểm tra lại trạng thái tệp nếu là CREATE hoặc MODIFY
                if (type != EventType.DELETE) {
                    File file = new File(rootPath.toFile(), normPath);
                    if (!file.exists() || !file.isFile()) return;
                }
                if (changeListener != null) {
                    changeListener.accept(type, normPath);
                }
            } catch (Exception ignored) {}
        }, 150, TimeUnit.MILLISECONDS);
    }
}
