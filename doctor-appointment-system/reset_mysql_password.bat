@echo off
echo ==========================================
echo  MySQL Password Reset Tool
echo ==========================================
echo.

:: Check for administrator privileges
net session >nul 2>&1
if %errorLevel% NEQ 0 (
    echo [ERROR] This script must be run as Administrator!
    echo Please right-click this file and select "Run as administrator".
    pause
    exit /b 1
)

echo [1/6] Stopping MySQL80 service...
net stop MySQL80 >nul 2>&1

echo [2/6] Creating password reset command...
echo ALTER USER 'root'@'localhost' IDENTIFIED BY 'root'; > "%TEMP%\mysql-init.txt"

echo [3/6] Starting MySQL temporarily to reset password...
start "" /B "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --defaults-file="C:\ProgramData\MySQL\MySQL Server 8.0\my.ini" --init-file="%TEMP%\mysql-init.txt" >nul 2>&1

echo [4/6] Waiting for password reset to complete...
timeout /t 10 /nobreak >nul

echo [5/6] Stopping temporary MySQL process...
taskkill /F /IM mysqld.exe >nul 2>&1
del "%TEMP%\mysql-init.txt" >nul 2>&1

echo [6/6] Restarting MySQL service normally...
net start MySQL80

echo.
echo ==========================================
echo  SUCCESS! 
echo  Your MySQL root password is now: root
echo ==========================================
pause
