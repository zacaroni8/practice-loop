#!/bin/bash
# PostToolUse automation: compile-check the whole source tree after any
# .java edit, so syntax errors surface immediately instead of only being
# caught the next time the app is run by hand.
#
# Assumes the Milestone 1 project layout: sources under src/, dependency
# jars (SQLite JDBC, JavaFX SDK) under lib/. Skips gracefully until both
# exist.

INPUT=$(cat)
FILE=$(echo "$INPUT" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*: *"//;s/"$//')

if [[ "$FILE" == *.java ]]; then
  if [ -d "src" ] && [ -d "lib" ]; then
    mkdir -p out
    javac -cp "lib/*" -d out $(find src -name "*.java") 2>&1 | head -30
  else
    echo "src/ or lib/ not set up yet — compile check will activate once Milestone 1 creates the project layout." >&2
  fi
fi

exit 0
