@echo off
if exist dist rmdir /S /Q dist
mkdir dist
mkdir dist\libs

REM Find jar tool
set JAR_CMD=jar
where jar >nul 2>&1
if errorlevel 1 (
    REM Try to find jar in JAVA_HOME
    if defined JAVA_HOME (
        set JAR_CMD=%JAVA_HOME%\bin\jar
    ) else (
        REM Get java.exe path and find jar
        for /f "tokens=*" %%i in ('where java 2^>nul') do (
            set JAVA_PATH=%%i
            goto :found_java
        )
        :found_java
        for %%i in ("!JAVA_PATH!") do set JAVA_DIR=%%~dpi
        set JAR_CMD=!JAVA_DIR!jar
    )
)

echo Manifest-Version: 1.0 > build\MANIFEST.MF
echo Main-Class: com.safmica.Launcher >> build\MANIFEST.MF

cd build
"%JAR_CMD%" cfm ..\dist\GotTheX.jar MANIFEST.MF *
if errorlevel 1 (
    cd ..
    echo ERROR: Failed to create JAR file!
    echo Please ensure JDK is installed and 'jar' command is available.
    exit /b 1
)
cd ..

copy lib\*.jar dist\libs\ >nul
copy lib\*.dll dist\libs\ >nul

REM Create launcher batch file
(
echo @echo off
echo set PATH=%%~dp0libs;%%PATH%%
echo java --module-path libs --add-modules javafx.controls,javafx.fxml -cp "libs\*;GotTheX.jar" com.safmica.Launcher
echo pause
) > dist\GotTheX.bat

exit /b 0
