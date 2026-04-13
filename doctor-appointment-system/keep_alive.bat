@echo off
:loop
echo [%date% %time%] 🚀 Starting Care Connect Backend Server...
:: Step 1: Ensure we are in the correct directory
cd /d "d:\Final_year_project\doctor-appointment-system"

:: Step 2: Try to run using Maven
call mvn spring-boot:run -Dspring-boot.run.profiles=local

:: Step 3: If Maven fails (e.g. not in path), try running the JAR directly
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Maven execution failed. Attempting to run from target JAR...
    java -jar target\doctor-appointment-system-1.0.0-SNAPSHOT.jar
)

echo [%date% %time%] ⚠️ Server stopped or crashed. Restarting in 5 seconds...
timeout /t 5
goto loop
