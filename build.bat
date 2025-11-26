@echo off
cls
echo ========================================
echo                GetTheX 
echo ========================================
echo.

set JAVAFX_URL=https://download2.gluonhq.com/openjfx/21.0.5/openjfx-21.0.5_windows-x64_bin-sdk.zip
set GSON_URL=https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar

if not exist lib mkdir lib
if not exist temp mkdir temp

echo [1/5] Downloading Gson...
if not exist lib\gson-2.10.1.jar (
    powershell -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%GSON_URL%' -OutFile 'lib\gson-2.10.1.jar'"
) else (
    echo Already exists
)

echo [2/5] Downloading JavaFX SDK...
if not exist lib\javafx.base.jar (
    powershell -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%JAVAFX_URL%' -OutFile 'temp\javafx.zip'"
    powershell -Command "Expand-Archive -Path 'temp\javafx.zip' -DestinationPath 'temp' -Force"
    for /r temp %%F in (javafx*.jar) do copy "%%F" lib\ >nul
    for /r temp %%F in (*.dll) do copy "%%F" lib\ >nul
    rmdir /S /Q temp
    mkdir temp
) else (
    echo Already exists
)

echo [3/5] Compiling...
call _compile.bat
if errorlevel 1 (
    pause
    exit /b 1
)

echo [4/5] Packaging...
call _package.bat

echo [5/5] Done!
echo.
echo Run: cd dist ^& GetTheX.bat
pause
