@echo off
cd /d "%~dp0"
if not exist "ChessMaster.jar" (
    echo ChessMaster.jar not found. Please compile first.
    pause
    exit /b 1
)
start "" javaw -jar ChessMaster.jar
