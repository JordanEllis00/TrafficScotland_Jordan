"""Independent oracle: computes the expected results from the fixture data (not from the app code).
Distances use the Vincenty ellipsoid formula (the app uses haversine, so the two share nothing)."""
import json, math, re
def vincenty(lat1, lon1, lat2, lon2):
    a=6378137.0; f=1/298.257223563; b=(1-f)*a
    L=math.radians(lon2-lon1); U1=math.atan((1-f)*math.tan(math.radians(lat1))); U2=math.atan((1-f)*math.tan(math.radians(lat2)))
    sU1,cU1,sU2,cU2=math.sin(U1),math.cos(U1),math.sin(U2),math.cos(U2); lam=L
    for _ in range(200):
        sl,cl=math.sin(lam),math.cos(lam)
        ss=math.sqrt((cU2*sl)**2+(cU1*sU2-sU1*cU2*cl)**2)
        if ss==0: return 0.0
        cs=sU1*sU2+cU1*cU2*cl; sig=math.atan2(ss,cs); sa=cU1*cU2*sl/ss; c2a=1-sa**2
        c2sm=cs-2*sU1*sU2/c2a if c2a else 0
        C=f/16*c2a*(4+f*(4-3*c2a)); lp=lam
        lam=L+(1-C)*f*sa*(sig+C*ss*(c2sm+C*cs*(-1+2*c2sm**2)))
        if abs(lam-lp)<1e-12: break
    u2=c2a*(a*a-b*b)/(b*b)
    A=1+u2/16384*(4096+u2*(-768+u2*(320-175*u2))); B=u2/1024*(256+u2*(-128+u2*(74-47*u2)))
    ds=B*ss*(c2sm+B/4*(cs*(-1+2*c2sm**2)-B/6*c2sm*(-3+4*ss**2)*(-3+4*c2sm**2)))
    return b*A*(sig-ds)/1000.0

td=json.load(open("fixtures/td1.json")); items=[]
for k,cat in (("U","UNPLANNED"),("R","ROADWORKS")):
    for s in td[k]:
        pt=s.get("pt") or (s.get("frm") if s.get("frm") else None)
        items.append(dict(sid=s["sid"],cat=cat,typ=s["typ"],road=s.get("road"),comment=" ".join(s["comments"]),pt=pt))
refs={"Glasgow":(55.8642,-4.2518),"Edinburgh":(55.9533,-3.1883),"Stirling":(56.1165,-3.9369),"Inverness":(57.4778,-4.2247)}
dist={r:{it["sid"]:round(vincenty(la,lo,*it["pt"]),1) for it in items if it["pt"]} for r,(la,lo) in refs.items()}
filters={f"{r}|{rad}":[(s,d) for d,s in sorted((d,s) for s,d in dist[r].items() if d<=rad)] for r in refs for rad in (10,25,50,100)}
def road_of(it):
    if it["road"]: return it["road"]
    m=re.search(r"\b([AM]\d{1,3}(?:\([M]\))?)\b",it["comment"].upper()); return m.group(1) if m else ""
def search(q):
    qn=q.strip().upper().replace(" ","")
    if re.fullmatch(r"[AM]\d{1,3}",qn):     # road code -> exact road, or whole-token in text
        rx=re.compile(r"(?<![A-Z0-9])"+qn+r"(?![0-9])")
        return [it["sid"] for it in items if road_of(it).upper()==qn or rx.search(it["comment"].upper())]
    return [it["sid"] for it in items if q.lower() in road_of(it).lower() or q.lower() in it["comment"].lower()]
queries=["M8","A9","a82","M74","A720","A8","A90","A","Fort William","Kilmarnock","Z99"]
sr={q:search(q) for q in queries}
def icon(it):
    t=it["typ"].lower()
    if it["cat"]=="ROADWORKS" or "maintenance" in t or "construction" in t: return "ROADWORKS"
    if "accident" in t: return "ACCIDENT"
    if "obstruction" in t or "nonweather" in t: return "OBSTRUCTION"
    if "management" in t or "closure" in t: return "CLOSURE"
    if "environment" in t or "weather" in t: return "WEATHER"
    if "traffic" in t or "congestion" in t: return "CONGESTION"
    if "event" in t or "activit" in t: return "EVENT"
    return "INFO"
icons={it["sid"]:icon(it) for it in items}
# safety: for the (city, radius) pairs used in tests, nothing lies within 1 km of a radius threshold, so haversine vs ellipsoid cannot change a result
TESTED={"Glasgow":(10,25,50,100),"Edinburgh":(50,),"Stirling":(25,)}   # (reference city, radii) pairs used by the tests
gaps=[(abs(d-rad),r,s,rad) for r,rads in TESTED.items() for s,d in dist[r].items() for rad in rads]
g=min(gaps); print("closest approach to a threshold: %.2f km (%s, %s, radius %s)"%g); assert g[0]>=1.0, "fixture too close to a radius threshold"
json.dump(dict(items=items,dist=dist,filters=filters,search=sr,icons=icons,min_gap=min(gaps)[0]),open("out/expected.json","w"),indent=1)
for k in ("Glasgow|10","Glasgow|25","Glasgow|50","Glasgow|100","Edinburgh|50","Stirling|25"): print(k,[(s[4:],d) for s,d in filters[k]])
for q,v in sr.items(): print(repr(q),[s[4:] for s in v])
from collections import Counter; print(Counter(icons.values()))
print("with coordinates:",sum(1 for i in items if i["pt"]),"of",len(items))
