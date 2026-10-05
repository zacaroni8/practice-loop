# Practice Loop

A desktop app for scheduling practice/work sessions, getting a real OS
notification before and when they start, and earning XP for completing
them. Built in Java with JavaFX and SQLite.

## 1. Install a JDK

You need a Java Development Kit, version 25 or newer. This project was
built and tested against **Eclipse Temurin 25**:

1. Go to https://adoptium.net/temurin/releases/
2. Pick version 25 (LTS), your OS, and the `.msi` installer (Windows) or
   the appropriate package for your OS.
3. Run the installer. During setup, make sure "Add to PATH" is checked.
4. Confirm it worked — open a terminal and run:
   ```
   java -version
   javac -version
   ```
   Both should print `25` (or higher).

## 2. Download the dependencies

These are **not** included in the repo (they're large binary files, kept
out of git on purpose) — you download them once, yourself:

1. Clone this repo, then create a `lib/` folder in its root if it
   doesn't already exist.
2. **SQLite JDBC driver** — download `sqlite-jdbc-3.53.4.0.jar` from
   https://github.com/xerial/sqlite-jdbc/releases and place it directly
   in `lib/`.
3. **JavaFX SDK** — go to https://gluonhq.com/products/javafx/, pick
   version 25, your OS, and the **SDK** download (not jmods). Unzip it,
   and place the resulting folder in `lib/`, renamed to `javafx-sdk` (so
   you end up with `lib/javafx-sdk/lib/javafx.controls.jar`, etc.).
4. *(Optional, only needed to run the automated tests)* **JUnit console
   standalone** — download
   `junit-platform-console-standalone-6.1.3.jar` from
   https://search.maven.org/artifact/org.junit.platform/junit-platform-console-standalone
   and place it in `lib/` too.

Your `lib/` folder should now look like:
```
lib/
  javafx-sdk/
    lib/
      javafx.controls.jar
      ...
  sqlite-jdbc-3.53.4.0.jar
  junit-platform-console-standalone-6.1.3.jar   (optional)
```

## 3. Run it

**Windows:** just run `run.bat` from the project root. It compiles and
launches the app in one step.

**Mac/Linux:** there's no equivalent script yet — run these two
commands from the project root manually (adjust `;` to `:` as a
classpath separator, which Java uses on non-Windows systems):
```
javac --module-path lib/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp "lib/*" -d out src/practiceloop/*.java
java --module-path lib/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp "out:lib/*" practiceloop.Main
```

The app stores its data in a `practiceloop.db` SQLite file created
next to wherever you run it from — it's created automatically on first
run.

## 4. Running the tests (optional)

```
java -jar lib/junit-platform-console-standalone-6.1.3.jar execute --class-path out --select-class practiceloop.Tester --details=tree
```
