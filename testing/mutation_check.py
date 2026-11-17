"""Mutation check: break the REAL source in a temp copy, run the tests, and confirm at least one test fails each time."""
import shutil, subprocess, os, re, sys, tempfile
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
MUTS = [
 ("M1 road search matches by substring (A9 also returns A90)", "model/TrafficSituation.java",
  "if (ROAD_CODE.matcher(q).matches()) {", "if (false) {"),
 ("M2 medium/long boundary moved (>7 becomes >8 days)", "model/TrafficSituation.java",
  "} else if (days <= 7) {", "} else if (days <= 8) {"),
 ("M3 distance filter inverted (> instead of <=)", "model/SituationFilter.java",
  "if (maxDistanceKm > 0 && km > maxDistanceKm) {", "if (maxDistanceKm > 0 && km < maxDistanceKm) {"),
 ("M4 results not sorted nearest-first", "model/SituationFilter.java",
  "result.sort(Comparator.comparingDouble(TrafficSituation::getDistanceKm));", ""),
 ("M5 unknown duration reported as SHORT", "model/TrafficSituation.java",
  "            return DurationSeverity.UNKNOWN;", "            return DurationSeverity.SHORT;"),
 ("M6 weather icon rule removed", "model/TrafficSituation.java",
  'else if (t.contains("environment") || t.contains("weather")) {', 'else if (false) {'),
 ("M7 parser ignores generalPublicComment 'value' joining", "parser/DatexXmlParser.java",
  'current.getGeneralPublicComment() + "\\n" + text', 'text'),
 ("M8 default comment text removed", "parser/DatexXmlParser.java",
  'current.setGeneralPublicComment("No public comment details provided by Traffic Scotland.");', ''),
 ("M9 truncated-download check always true", "parser/DatexXmlParser.java",
  'return t.startsWith("<") && t.endsWith("d2LogicalModel>");', 'return true;'),
 ("M10 category filter ignores roadworks-only", "model/SituationFilter.java",
  "category == Category.ROADWORKS_ONLY && item.getFeedCategory() != TrafficSituation.FeedCategory.CURRENT_ROADWORK", "false"),
]
killed = 0
for name, rel, old, new in MUTS:
    tmp = tempfile.mkdtemp()
    shutil.copytree(ROOT, os.path.join(tmp, "p"), ignore=shutil.ignore_patterns("out", "build", ".gradle", ".idea"))
    f = os.path.join(tmp, "p/app/src/main/java/org/me/gcu/trafficscotland_jordan", rel)
    src = open(f, encoding="utf-8").read()
    assert old in src, "mutation target not found: " + name
    open(f, "w", encoding="utf-8").write(src.replace(old, new, 1))
    r = subprocess.run(["bash", os.path.join(tmp, "p/testing/run_tests.sh")], capture_output=True, text=True)
    fails = re.findall(r"FAIL\s+(\w+)", r.stdout)
    status = "KILLED " if fails else "SURVIVED"
    killed += bool(fails)
    print(f"{status} {name}\n         failing tests: {', '.join(fails) if fails else '(none)'}")
    shutil.rmtree(tmp, ignore_errors=True)
print(f"\nMutants killed: {killed} of {len(MUTS)}")
