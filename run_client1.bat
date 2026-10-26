@echo off
chcp 65001 >nul
echo Khởi chạy Máy khách Client 1 (Client-User1)...
if not exist client_storage_1 (
    mkdir client_storage_1
)
start "FileSync Client 1 - Nhóm 14" javaw -cp bin com.filesync.Main --client Client-User1 client_storage_1
