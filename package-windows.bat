@echo off
REM Builds a standalone "Practice Loop.exe" app folder under dist\ - no JDK,
REM no lib\ setup needed for whoever runs the result. Run with: package-windows.bat
REM
REM One-time setup this needs beyond the usual lib\ folder: a JavaFX "jmods"
REM download (NOT the same as the javafx-sdk used by run.bat) at
REM lib\javafx-jmods\ - get it from https://gluonhq.com/products/javafx/,
REM same version/OS, but choose the jmods package type instead of SDK.

REM 1. Compile (same invocation as run.bat).
javac --module-path lib\javafx-sdk\lib --add-modules javafx.controls,javafx.fxml -cp "lib/*" -d out src\practiceloop\*.java
if errorlevel 1 exit /b 1

REM 2. Bundle the compiled classes into a runnable jar, so jpackage has one
REM    thing to point at instead of a loose folder of .class files.
if exist dist-input rmdir /s /q dist-input
mkdir dist-input
jar cfe dist-input\practiceloop.jar practiceloop.Main -C out .
if errorlevel 1 exit /b 1

REM 3. jpackage needs the SQLite driver sitting next to the app jar too -
REM    it goes on the app's classpath, same as -cp "out;lib/*" does for run.bat.
REM    style.css ships the same way - Main.java resolves both it and
REM    practiceloop.db against the app.dir system property set in step 5
REM    below, not a relative/working-directory path (a real working
REM    directory can't be trusted for a double-clicked native app - confirmed
REM    this breaks on macOS, where it resets to "/" unconditionally).
copy lib\sqlite-jdbc-3.53.4.0.jar dist-input\ >nul
if errorlevel 1 exit /b 1
copy style.css dist-input\ >nul
if errorlevel 1 exit /b 1

REM 4. Build a custom runtime image with jlink, using the real JavaFX jmods
REM    (not the SDK's plain jars - jlink needs actual .jmod files to bundle
REM    JavaFX's native rendering libraries correctly; without them the
REM    packaged app fails at startup with "no suitable pipeline found").
REM    java.sql is included because the app talks to SQLite over JDBC.
if exist javafx-runtime-image rmdir /s /q javafx-runtime-image
jlink --module-path lib\javafx-jmods --add-modules javafx.controls,javafx.fxml,java.sql ^
    --output javafx-runtime-image --strip-debug --no-header-files --no-man-pages
if errorlevel 1 exit /b 1

REM 5. Build the app-image: a folder with "Practice Loop.exe" inside, the
REM    runtime image from step 4, and the dependency jars from step 2/3 -
REM    nothing else required on the machine that runs it.
if exist dist rmdir /s /q dist
jpackage --type app-image --name "Practice Loop" --app-version 2.0.0 ^
    --input dist-input --main-jar practiceloop.jar --main-class practiceloop.Main ^
    --runtime-image javafx-runtime-image --dest dist ^
    --java-options "-Dapp.dir=$APPDIR"
if errorlevel 1 exit /b 1

echo.
echo Done - see dist\Practice Loop\Practice Loop.exe
