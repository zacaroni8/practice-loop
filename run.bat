@echo off
REM Compiles and launches Practice Loop. Run with: run.bat

javac --module-path lib\javafx-sdk\lib --add-modules javafx.controls,javafx.fxml -cp "lib/*" -d out src\practiceloop\*.java
if errorlevel 1 exit /b 1

java --module-path lib\javafx-sdk\lib --add-modules javafx.controls,javafx.fxml -cp "out;lib/*" practiceloop.Main
