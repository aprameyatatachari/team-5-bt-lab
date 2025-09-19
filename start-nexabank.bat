@echo off
echo Starting NexaBank Application Services...
echo.

REM Get the current directory (should be the root of team-5-bt-lab)
set ROOT_DIR=%cd%

echo Starting Login Module Backend (Port 8080)...
start "Login Module Backend" powershell -Command "cd '%ROOT_DIR%\login-module'; mvn spring-boot:run"

echo Starting Customer Module Backend (Port 8081)...
start "Customer Module Backend" powershell -Command "cd '%ROOT_DIR%\customer-module'; mvn spring-boot:run"

echo Starting Login Module Frontend...
start "Login Module Frontend" powershell -Command "cd '%ROOT_DIR%\login-module\ui'; npm run dev"

echo Starting Customer Module Frontend...
start "Customer Module Frontend" powershell -Command "cd '%ROOT_DIR%\customer-module\ui'; npm run dev"

echo.
echo ============================================
echo All services are starting in separate windows:
echo - Login Backend:     http://localhost:8080
echo - Customer Backend:  http://localhost:8081  
echo - Login Frontend:    http://localhost:5173
echo - Customer Frontend: http://localhost:5174
echo ============================================
echo.
echo Press any key to close this window...
pause >nul
