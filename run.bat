@echo off
setlocal
cd /d "%~dp0"
where javac >nul 2>&1
if errorlevel 1 (
    echo Java JDK 21 is required. Add javac to PATH and try again.
    pause
    exit /b 1
)
if not exist "build\classes" mkdir "build\classes"
javac --release 21 -encoding UTF-8 -d "build\classes" src\management\*.java
if errorlevel 1 (
    pause
    exit /b 1
)
java -cp "build\classes" management.Main %*
