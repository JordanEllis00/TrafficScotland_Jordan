#!/bin/bash
# Command-line harness: compiles the REAL model/parser/filter sources with the shims in testing/shim and runs the unit tests.
# Usage (from the project root):  bash testing/run_tests.sh
set -e
unset JAVA_TOOL_OPTIONS
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
P="$ROOT/app/src/main/java/org/me/gcu/trafficscotland_jordan"
T="$ROOT/app/src/test/java/org/me/gcu/trafficscotland_jordan"
OUT="${TMPDIR:-/tmp}/mpd_harness_classes"
rm -rf "$OUT" && mkdir -p "$OUT"
JAVAC="$(command -v javac || echo "java -m jdk.compiler/com.sun.tools.javac.Main")"
$JAVAC -d "$OUT" $(find "$ROOT/testing/shim" -name '*.java') "$P/model/TrafficSituation.java" "$P/model/SituationFilter.java" \
      "$P/parser/DatexXmlParser.java" "$T"/*.java "$ROOT/testing/Runner.java" "$ROOT/testing/Dump.java" "$ROOT/testing/FilterDump.java" 2>&1 | grep -v "^Picked up" || true
cd "$ROOT/app"
java -cp "$OUT:src/test/resources" org.me.gcu.trafficscotland_jordan.Runner
