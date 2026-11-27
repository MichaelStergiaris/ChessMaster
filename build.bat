@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist "out" mkdir out

echo Collecting Java source files...
dir /s /b src\*.java > sources.txt

echo Compiling with javac...
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    del sources.txt
    echo Compilation failed!
    exit /b 1
)
del sources.txt

set "JAR_CMD=jar"
where jar >nul 2>&1
if errorlevel 1 (
    for /d %%D in ("C:\Program Files\Java\jdk*") do (
        if exist "%%D\bin\jar.exe" set "JAR_CMD=%%D\bin\jar.exe"
    )
)

echo Packaging ChessMaster.jar...
"%JAR_CMD%" cfe ChessMaster.jar com.chessmaster.ChessMasterApp -C out .
if errorlevel 1 (
    echo Packaging JAR failed!
    exit /b 1
)

echo Build succeeded! Run run.bat to launch ChessMaster.
endlocal
