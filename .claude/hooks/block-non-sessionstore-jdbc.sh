#!/bin/bash
# PreToolUse guardrail: only SessionStore.java may use JDBC/SQL directly.
# Enforces the rule in CLAUDE.md's Project Conventions with a real block,
# not just a reminder that's easy to forget mid-implementation.

INPUT=$(cat)
FILE=$(echo "$INPUT" | grep -o '"file_path"[[:space:]]*:[[:space:]]*"[^"]*"' | head -1 | sed 's/.*: *"//;s/"$//')

if [[ "$FILE" == *.java && "$FILE" != *SessionStore.java ]]; then
  if echo "$INPUT" | grep -qE 'java\.sql\.|DriverManager|PreparedStatement|ResultSet'; then
    echo "Blocked: only SessionStore.java may use JDBC/SQL directly (see CLAUDE.md Project Conventions)." >&2
    exit 2
  fi
fi

exit 0
