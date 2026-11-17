"""Compares the app's actual search/distance results (out/filter_dump.jsonl) with the independent oracle (out/expected.json)."""
import json
e = json.load(open('out/expected.json')); bad = 0; rows = []
for l in open('out/filter_dump.jsonl'):
    r = json.loads(l)
    if r['kind'] == 'search':
        exp = [s[4:] for s in e['search'][r['query']]]; ok = exp == r['result']
        rows.append(("search", r['query'], exp, r['result'], ok))
    elif r['kind'] == 'distance':
        ex = e['filters'][f"{r['ref']}|{r['radius']}"]; got = [x.split() for x in r['result']]
        ids_ok = [s[4:] for s, d in ex] == [g[0] for g in got]
        dist_ok = all(abs(float(g[1]) - d) <= max(0.3, 0.005 * d) for g, (s, d) in zip(got, ex)) if ids_ok else False
        ok = ids_ok and dist_ok
        rows.append(("distance", f"{r['ref']} <= {r['radius']} km", [f"{s[4:]} {d}" for s, d in ex], r['result'], ok))
    bad += (rows[-1][4] is False) if r['kind'] in ('search', 'distance') else 0
for k, q, ex, got, ok in rows: print(f"{'OK ' if ok else 'BAD'} {k:9s} {q:22s} expected={ex} actual={got}")
print("mismatches vs oracle:", bad)
json.dump([dict(kind=k, q=q, expected=ex, actual=got, ok=ok) for k, q, ex, got, ok in rows], open('out/filter_compare.json', 'w'), indent=1)
