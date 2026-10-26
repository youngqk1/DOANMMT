@echo off
chcp 65001 >nul
echo ======================================================================
echo   BIÊN DỊCH DỰ ÁN ĐỒ ÁN MẠNG MÁY TÍNH - NHÓM 14
echo   Đề tài: Ứng dụng Đồng bộ Tệp tin (Client-Server File Synchronization)
echo ======================================================================

if not exist bin (
    mkdir bin
)

echo [1/2] Đang xóa bytecode cũ trong bin/...
del /s /q bin\*.class >nul 2>&1

echo [2/2] Đang biên dịch mã nguồn Java với javac...
javac -encoding UTF-8 -d bin src\com\filesync\common\*.java src\com\filesync\server\*.java src\com\filesync\client\*.java src\com\filesync\test\*.java src\com\filesync\Main.java

if %ERRORLEVEL% equ 0 (
    echo ======================================================================
    echo   BIÊN DỊCH THÀNH CÔNG 100%! BYTECODE ĐÃ ĐƯỢC TẠO TẠI THƯ MỤC bin/
    echo ======================================================================
) else (
    echo ======================================================================
    echo   [LỖI] BIÊN DỊCH THẤT BẠI! Vui lòng kiểm tra lại mã nguồn.
    echo ======================================================================
)
pause
