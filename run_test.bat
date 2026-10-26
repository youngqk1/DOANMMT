@echo off
chcp 65001 >nul
echo Đang chạy kịch bản kiểm thử mô phỏng tự động...
if not exist bin\com\filesync\Main.class (
    echo Đang biên dịch mã nguồn trước khi chạy...
    call compile.bat
)
java -cp bin com.filesync.test.MultiClientSimulationTest
pause
