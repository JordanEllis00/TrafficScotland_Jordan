"""Independent cross-check: re-reads the raw feed XML with Python's ElementTree (no code shared with the app) and compares
every field the app displays with the value the app's parser produced (JSON lines written by Dump.java).
Usage: python3 crosscheck.py <res/raw dir>   (run from the testing folder after dump_all.sh)"""
import json, re, sys, datetime, math
import xml.etree.ElementTree as ET
def ln(t): return t.split('}')[-1].split(':')[-1]
def fmt_type(t): return re.sub(r'([a-z])([A-Z])', r'\1 \2', t.split(':')[-1])
def fmt_dt(s):
    return "Not specified" if not s else datetime.datetime.fromisoformat(s).strftime("%d %b %Y, %H:%M")
def days(a, b):
    if not a or not b: return -1
    h = (datetime.datetime.fromisoformat(b) - datetime.datetime.fromisoformat(a)).total_seconds() // 3600
    return max(1, math.ceil(h / 24))
def src(path):
    root = ET.parse(path).getroot(); out = []
    for sit in root.iter():
        if ln(sit.tag) != 'situation': continue
        d = dict(sid=sit.get('id'), start=None, end=None, pt=None, frm=None, to=None, rid=None, typ=None, road=None)
        comments = []
        for e in sit.iter():
            n = ln(e.tag)
            if n == 'situationRecord':
                d['rid'] = e.get('id'); d['typ'] = [v for k, v in e.attrib.items() if k.endswith('type')][0]
            elif n == 'value' and (e.text or '').strip(): comments.append(e.text.strip())
            elif n == 'overallStartTime': d['start'] = e.text.strip()
            elif n == 'overallEndTime': d['end'] = e.text.strip()
            elif n == 'locationForDisplay':
                d['pt'] = (float([x for x in e if ln(x.tag) == 'latitude'][0].text), float([x for x in e if ln(x.tag) == 'longitude'][0].text))
            elif n == 'linearCoordinatesStartPoint':
                p = [x for x in e.iter() if ln(x.tag) in ('latitude', 'longitude')]; d['frm'] = (float(p[0].text), float(p[1].text))
            elif n == 'linearCoordinatesEndPoint':
                p = [x for x in e.iter() if ln(x.tag) in ('latitude', 'longitude')]; d['to'] = (float(p[0].text), float(p[1].text))
            elif n == 'roadNumber': d['road'] = e.text.strip()
        d['comment'] = "\n".join(comments) or "No public comment details provided by Traffic Scotland."
        out.append(d)
    return out
def check(path, jsonl):
    S = src(path); P = [json.loads(l) for l in open(jsonl, encoding='utf-8')]
    assert len(S) == len(P), f"count mismatch {len(S)} vs {len(P)}"
    rows = []; bad = 0
    for s, p in zip(S, P):
        checks = [("situation id", s['sid'], p['sid']), ("record id", s['rid'], p['rid']),
                  ("type", fmt_type(s['typ']), p['type']), ("comment", s['comment'], p['comment']),
                  ("start (displayed)", fmt_dt(s['start']), p['startFmt']), ("end (displayed)", fmt_dt(s['end']), p['endFmt']),
                  ("lat/lon", s['pt'], (p['lat'], p['lon']) if p['hasPoint'] else None)]
        if s['frm'] or s['to']:
            checks.append(("linear from/to", (s['frm'], s['to']), ((p['fLat'], p['fLon']), (p['tLat'], p['tLon']))))
        if p['cat'] == 'CURRENT_ROADWORK': checks.append(("duration days", days(s['start'], s['end']), p['days']))
        if s.get('road'): checks.append(("road (tag)", s['road'], p['road']))
        res = []
        for name, a, b in checks:
            ok = (a == b)
            if not ok: bad += 1
            res.append((name, str(a), str(b), ok))
        rows.append((s['sid'], res))
    return rows, sum(len(r) for _, r in rows), bad
if __name__ == "__main__":
    raw = sys.argv[1]; allres = {}
    for label, path, jl in [("TD-0 unplanned (res/raw)", raw + "/sample_unplanned.xml", "out/td0_unplanned.jsonl"),
                            ("TD-0 roadworks (res/raw)", raw + "/sample_roadworks.xml", "out/td0_roadworks.jsonl"),
                            ("TD-1 unplanned", "fixtures/TD1_unplanned.xml", "out/td1_unplanned.jsonl"),
                            ("TD-1 roadworks", "fixtures/TD1_roadworks.xml", "out/td1_roadworks.jsonl")]:
        rows, n, bad = check(path, jl)
        allres[label] = dict(rows=[(a, [list(x) for x in b]) for a, b in rows], n=n, bad=bad)
        print(f"{label:28s} situations={len(rows):2d} field-comparisons={n:3d} mismatches={bad}")
        for sid, res in rows:
            for name, a, b, ok in res:
                if not ok: print("   MISMATCH", sid, name, "src=", a, "app=", b)
    json.dump(allres, open("out/crosscheck.json", "w"), indent=1)
