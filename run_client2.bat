@echo off
chcp 65001 >nul
echo Khởi chạy Máy khách Client 2 (Client-User2)...
if not exist client_storage_2 (
    mkdir client_storage_2
)
start "FileSync Client 2 - Nhóm 14" javaw -cp bin com.filesync.Main --client Client-User2 client_storage_2
