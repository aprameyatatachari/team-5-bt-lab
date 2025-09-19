@echo off
title NexaBank Application Launcher
color 0A

echo.
echo  ███╗   ██╗███████╗██╗  ██╗ █████╗ ██████╗  █████╗ ███╗   ██╗██╗  ██╗
echo  ████╗  ██║██╔════╝╚██╗██╔╝██╔══██╗██╔══██╗██╔══██╗████╗  ██║██║ ██╔╝
echo  ██╔██╗ ██║█████╗   ╚███╔╝ ███████║██████╔╝███████║██╔██╗ ██║█████╔╝ 
echo  ██║╚██╗██║██╔══╝   ██╔██╗ ██╔══██║██╔══██╗██╔══██║██║╚██╗██║██╔═██╗ 
echo  ██║ ╚████║███████╗██╔╝ ██╗██║  ██║██████╔╝██║  ██║██║ ╚████║██║  ██╗
echo  ╚═╝  ╚═══╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═══╝╚═╝  ╚═╝
echo.
echo                          Banking Technology Lab
echo                         Application Startup Script
echo.

REM Check if we're in the right directory
if not exist "login-module" (
    echo ERROR: Please run this script from the team-5-bt-lab root directory
    echo Current directory: %cd%
    echo Expected structure: team-5-bt-lab\login-module and team-5-bt-lab\customer-module
    pause
    exit /b 1
)

set ROOT_DIR=%cd%

echo [1/4] Starting Login Module Backend (Spring Boot)...
start "🏦 Login Backend - Port 8080" powershell -NoExit -Command "Write-Host 'Starting Login Module Backend...' -ForegroundColor Green; cd '%ROOT_DIR%\login-module'; mvn spring-boot:run"

timeout /t 3 /nobreak >nul

echo [2/4] Starting Customer Module Backend (Spring Boot)...
start "👥 Customer Backend - Port 8081" powershell -NoExit -Command "Write-Host 'Starting Customer Module Backend...' -ForegroundColor Blue; cd '%ROOT_DIR%\customer-module'; mvn spring-boot:run"

timeout /t 3 /nobreak >nul

echo [3/4] Starting Login Module Frontend (React/Vite)...
start "🔐 Login Frontend - Port 5173" powershell -NoExit -Command "Write-Host 'Starting Login Module Frontend...' -ForegroundColor Yellow; cd '%ROOT_DIR%\login-module\ui'; npm run dev"

timeout /t 2 /nobreak >nul

echo [4/4] Starting Customer Module Frontend (React/Vite)...
start "💼 Customer Frontend - Port 5174" powershell -NoExit -Command "Write-Host 'Starting Customer Module Frontend...' -ForegroundColor Cyan; cd '%ROOT_DIR%\customer-module\ui'; npm run dev"

echo.
echo ✅ All services are starting in separate PowerShell windows!
echo.
echo 📊 Service Dashboard:
echo ┌─────────────────────┬──────────────────────┬─────────────────────┐
echo │ Service             │ URL                  │ Status Window       │
echo ├─────────────────────┼──────────────────────┼─────────────────────┤
echo │ Login Backend       │ http://localhost:8080│ 🏦 Login Backend    │
echo │ Customer Backend    │ http://localhost:8081│ 👥 Customer Backend │
echo │ Login Frontend      │ http://localhost:5173│ 🔐 Login Frontend   │
echo │ Customer Frontend   │ http://localhost:5174│ 💼 Customer Frontend│
echo └─────────────────────┴──────────────────────┴─────────────────────┘
echo.
echo 🔧 Troubleshooting:
echo - Backend services may take 30-60 seconds to fully start
echo - Frontend services usually start in 10-20 seconds  
echo - Check individual windows if a service fails to start
echo - Ensure ports 8080, 8081, 5173, 5174 are available
echo.
echo 🛑 To stop all services: Close each PowerShell window individually
echo.
echo Press any key to close this launcher window...
pause >nul
