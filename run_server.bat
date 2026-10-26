@echo off
chcp 65001 >nul
echo Khởi chạy Máy chủ Đồng bộ Tệp tin (Server GUI)...
if not exist bin\com\filesync\Main.class (
    echo Đang biên dịch mã nguồn trước khi chạy...
    call compile.bat
)
start "FileSync Server - Nhóm 14" javaw -cp bin com.filesync.Main --server
