#!/bin/bash
# Static verification of structural requirements (run from anywhere). Output is reproduced in the test report.
cd "$(dirname "$0")/../app"
J=src/main/java/org/me/gcu/trafficscotland_jordan; R=src/main/res
echo '$ S-01  grep -rnE "runOnUiThread|AsyncTask" src/main/java'
grep -rnE "runOnUiThread|AsyncTask" src/main/java; echo "(exit status $? ; 1 = no matches)"; echo
echo '$ S-01b background work + result delivery'
grep -rnE "ExecutorService|scheduleWithFixedDelay|postValue|AtomicBoolean|shutdownNow" $J | sed "s#$J/##" | cut -c1-120; echo
echo '$ S-02  ls src/main/res/layout src/main/res/layout-land'; ls $R/layout $R/layout-land; echo
echo '$ S-02b view ids only in portrait activity_main ("<") or only in landscape (">")'
diff <(grep -o '@+id/[A-Za-z]*' $R/layout/activity_main.xml | sort) <(grep -o '@+id/[A-Za-z]*' $R/layout-land/activity_main.xml | sort) | grep '^[<>]'; echo
echo '$ S-02c landscape split weights'; grep -nE 'layout_weight="0\.(46|54)"' $R/layout-land/activity_main.xml; echo
echo '$ S-03  ls src/main/res/drawable | grep "^ic_" (excluding launcher)'; ls $R/drawable | grep -E '^ic_' | grep -v launcher; echo
echo '$ S-03b icon mapping (SituationStyle.iconFor)'; grep -n "case .*return R.drawable\|default:.*R.drawable" $J/view/SituationStyle.java | sed 's/^ *//' ; echo
echo '$ S-04  manifest permissions'; grep -n "uses-permission" src/main/AndroidManifest.xml | sed 's/^ *//'; echo
echo '$ S-05  java sources'; find src/main/java -name '*.java' | sed 's#src/main/java/##' | sort; echo
echo '$ S-06  XmlPullParser usage'; grep -n "XmlPullParser\b" $J/parser/DatexXmlParser.java | head -4; echo
echo '$ S-07  refresh scheduling'; grep -n "AUTO_REFRESH_MINUTES\|refreshData();" $J/repository/TrafficRepository.java $J/viewmodel/TrafficViewModel.java | sed "s#$J/##"; echo
echo '$ S-08  no credentials stored in source: default values in build.gradle.kts'
grep -n 'getProperty("DATEX_CLIENT' build.gradle.kts
echo '$ search source for key-like strings (40+ chars mixing letters and digits)'
grep -rIoE "[A-Za-z0-9_-]{40,}" build.gradle.kts src/main 2>/dev/null | grep -E "[A-Za-z]" | grep -E "[0-9]" | grep -vE "^[^:]*:-+$" ; echo "(exit status $? ; 1 = none found)"; echo
echo '$ S-08b .gitignore entries'; grep -nE "credentials.properties|local.properties|/app/build" ../.gitignore; echo
echo '$ S-09  connectivity check + completeness check before caching'; grep -n "isNetworkAvailable\|isCompleteDocument" $J/repository/TrafficRepository.java | sed "s#$J/##"
