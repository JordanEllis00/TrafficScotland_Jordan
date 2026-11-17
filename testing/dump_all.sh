#!/bin/bash
# Writes one JSON line per parsed situation for each feed (needs run_tests.sh to have compiled the classes first)
unset JAVA_TOOL_OPTIONS
cd "$(dirname "$0")"
CP="${TMPDIR:-/tmp}/mpd_harness_classes"; RAW=../app/src/main/res/raw
J="java -Dstdout.encoding=UTF-8 -cp $CP org.me.gcu.trafficscotland_jordan.Dump"
$J $RAW/sample_unplanned.xml U > out/td0_unplanned.jsonl
$J $RAW/sample_roadworks.xml R > out/td0_roadworks.jsonl
$J fixtures/TD1_unplanned.xml U > out/td1_unplanned.jsonl
$J fixtures/TD1_roadworks.xml R > out/td1_roadworks.jsonl
wc -l out/*.jsonl
(cd ../app && java -cp "$CP:src/test/resources" org.me.gcu.trafficscotland_jordan.FilterDump) > out/filter_dump.jsonl
