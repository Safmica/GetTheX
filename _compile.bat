@echo off
if exist build rmdir /S /Q build
mkdir build

setlocal enabledelayedexpansion
set CP=
for %%F in (lib\*.jar) do set CP=!CP!;%%F

dir /s /B src\main\java\*.java > sources.txt
javac -d build -cp "%CP%" --module-path lib --add-modules javafx.controls,javafx.fxml @sources.txt
set RESULT=%ERRORLEVEL%
del sources.txt

if %RESULT% NEQ 0 exit /b 1

xcopy /E /I /Y src\main\resources\* build\ >nul
exit /b 0
