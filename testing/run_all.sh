#!/bin/bash
# Regenerates every piece of test evidence from the current source. Run from the project root: bash testing/run_all.sh
set -e; cd "$(dirname "$0")"; unset JAVA_TOOL_OPTIONS
python3 -I gen_fixtures.py && cp fixtures/TD1_unplanned.xml fixtures/TD1_roadworks.xml fixtures/malformed.xml ../app/src/test/resources/
python3 -I expected.py > out/oracle_summary.txt
bash run_tests.sh | tee out/unit_test_run.txt
bash dump_all.sh > /dev/null
python3 -I crosscheck.py ../app/src/main/res/raw | tee out/crosscheck_summary.txt
python3 -I compare_filters.py | tee out/filter_compare.txt
bash static_checks.sh > out/static_checks.txt 2>&1
python3 -I mutation_check.py | tee out/mutation_check.txt
