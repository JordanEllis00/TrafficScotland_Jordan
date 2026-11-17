"""Generates the TD-1 test fixtures (DATEX II-style XML) and td1.json (the same data, used to compute expected values).
Run:  python3 gen_fixtures.py   (writes into ./fixtures)"""
import json, re, os
from xml.sax.saxutils import escape

U = [
 dict(sid="SIT_U_1001", rid="REC_U_1001", typ="Accident", road="M8", sev="high", impact="laneBlocked",
      comments=["M8 Eastbound Junction 15 (Townhead) & Junction 16 - Lane 1 blocked due to a multi-vehicle collision. Delays of approximately 20 minutes."],
      start="2026-11-17T09:15:00+00:00", end="2026-11-17T16:30:00+00:00", pt=(55.8674,-4.2361)),
 dict(sid="SIT_U_1002", rid="REC_U_1002", typ="AbnormalTraffic", road=None, sev="medium", impact=None,
      comments=["A9 Northbound approaching Keir roundabout - slow moving traffic due to high volume."],
      start="2026-11-17T08:00:00+00:00", end="2026-11-17T12:00:00+00:00", pt=(56.1500,-3.9600)),
 dict(sid="SIT_U_1003", rid="REC_U_1003", typ="VehicleObstruction", road="A90", sev="low", impact="laneBlocked",
      comments=["A90 Southbound near Dundee Kingsway - broken down vehicle on hard shoulder."],
      start="2026-11-17T10:40:00+00:00", end="2026-11-17T13:00:00+00:00", pt=(56.4890,-2.9900)),
 dict(sid="SIT_U_1004", rid="REC_U_1004", typ="RoadOrCarriagewayOrLaneManagement", road=None, sev="high", impact="roadClosed",
      comments=["A96 Eastbound between Smithton and Raigmore - road closed for emergency repairs."],
      start="2026-11-17T06:00:00+00:00", end="2026-11-17T18:00:00+00:00", pt=(57.5000,-4.0600),
      frm=(57.4880,-4.0900), to=(57.5120,-4.0300)),
 dict(sid="SIT_U_1005", rid="REC_U_1005", typ="Accident", road=None, sev=None, impact=None,
      comments=[], start="2026-11-17T11:00:00+00:00", end="2026-11-17T14:00:00+00:00", pt=None),
 dict(sid="SIT_U_1006", rid="REC_U_1006", typ="Obstruction", road="A82", sev="medium", impact=None,
      comments=["A82 Fort William - fallen tree blocking northbound carriageway.", "Recovery vehicle en route."],
      start="2026-11-17T07:30:00+00:00", end="2026-11-17T15:00:00+00:00", pt=(56.8198,-5.1052)),
 dict(sid="SIT_U_1007", rid="REC_U_1007", typ="PoorEnvironmentConditions", road="A9", sev="medium", impact=None,
      comments=["A9 Drumochter Pass - snow and ice, drive with care."],
      start="2026-11-17T05:00:00+00:00", end="2026-11-17T20:00:00+00:00", pt=(56.8700,-4.2400)),
 dict(sid="SIT_U_1008", rid="REC_U_1008", typ="PublicEvent", road=None, sev="low", impact=None,
      comments=["Murrayfield match day - heavy traffic expected around the A8 and Edinburgh City Bypass."],
      start="2026-11-17T12:00:00+00:00", end="2026-11-17T21:00:00+00:00", pt=(55.9420,-3.2430)),
 dict(sid="SIT_U_1009", rid="REC_U_1009", typ="GeneralInstructionOrMessageToRoadUsers", road="M80", sev=None, impact=None,
      comments=["M80 Northbound - temporary 40 mph speed limit in force."],
      start="2026-11-17T09:00:00+00:00", end="2026-11-17T17:00:00+00:00", pt=(55.9300,-4.0100)),
]
R = [
 dict(sid="SIT_R_2001", rid="REC_R_2001", typ="MaintenanceWorks", road="M8", impact="laneClosures", mt="resurfacing",
      comments=["M8 Westbound Junction 5 (Harthill) - overnight resurfacing, lane closures in operation."],
      start="2026-11-17T20:00:00+00:00", end="2026-11-18T06:00:00+00:00", pt=(55.8480,-3.7400), frm=(55.8470,-3.7200), to=(55.8490,-3.7600)),
 dict(sid="SIT_R_2002", rid="REC_R_2002", typ="MaintenanceWorks", road="A77", impact="laneClosures", mt="drainage",
      comments=["A77 Southbound at Kilmarnock bypass - drainage repairs, temporary traffic lights."],
      start="2026-11-16T00:00:00+00:00", end="2026-11-21T00:00:00+00:00", pt=(55.5900,-4.5300)),
 dict(sid="SIT_R_2003", rid="REC_R_2003", typ="ConstructionWorks", road="M74", impact="contraflow", mt=None,
      comments=["M74 Northbound near Abington - bridge construction, contraflow in place."],
      start="2026-11-02T00:00:00+00:00", end="2026-12-02T00:00:00+00:00", pt=(55.4800,-3.6930), frm=(55.4700,-3.6700), to=(55.4900,-3.7200)),
 dict(sid="SIT_R_2004", rid="REC_R_2004", typ="MaintenanceWorks", road="A9", impact="laneClosures", mt="roadsideWork",
      comments=["A9 Perth to Inveralmond - carriageway improvement scheme."],
      start="2026-10-20T00:00:00+00:00", end="2026-12-20T00:00:00+00:00", pt=(56.3950,-3.4308)),
 dict(sid="SIT_R_2005", rid="REC_R_2005", typ="MaintenanceWorks", road="A82", impact="laneClosures", mt="roadsideWork",
      comments=["A82 Fort William - verge and drainage work (7 day boundary case)."],
      start="2026-11-15T00:00:00+00:00", end="2026-11-22T00:00:00+00:00", pt=(56.8160,-5.1120)),
 dict(sid="SIT_R_2006", rid="REC_R_2006", typ="MaintenanceWorks", road="A90", impact="laneClosures", mt="resurfacing",
      comments=["A90 Aberdeen Western Peripheral junction - resurfacing (2 day boundary case)."],
      start="2026-11-17T08:00:00+00:00", end="2026-11-19T08:00:00+00:00", pt=(57.1300,-2.0900)),
 dict(sid="SIT_R_2007", rid="REC_R_2007", typ="MaintenanceWorks", road=None, impact=None, mt=None,
      comments=["A720 Edinburgh City Bypass - ongoing barrier works (no end date supplied)."],
      start="2026-11-10T00:00:00+00:00", end=None, pt=(55.9100,-3.2300)),
 dict(sid="SIT_R_2008", rid="REC_R_2008", typ="MaintenanceWorks", road="M90", impact="laneClosures", mt="resurfacing",
      comments=["M90 Northbound near Kinross - resurfacing (8 day boundary case)."],
      start="2026-11-10T00:00:00+00:00", end="2026-11-18T00:00:00+00:00", pt=(56.2400,-3.4200)),
 dict(sid="SIT_R_2009", rid="REC_R_2009", typ="MaintenanceWorks", road="A1", impact="laneClosures", mt="drainage",
      comments=["A1 Southbound near Dunbar - gully clearing (3 day boundary case)."],
      start="2026-11-17T00:00:00+00:00", end="2026-11-19T01:00:00+00:00", pt=(55.9970,-2.5160)),
]

def loc_xml(s, p, ind):
    sp = " " * ind
    if s.get("pt") is None:
        return f'{sp}<{p}groupOfLocations xsi:type="{p}Point"/>\n'
    t = "Linear" if s.get("frm") else "Point"
    o = f'{sp}<{p}groupOfLocations xsi:type="{p}{t}">\n'
    o += f'{sp}  <{p}locationForDisplay><{p}latitude>{s["pt"][0]}</{p}latitude><{p}longitude>{s["pt"][1]}</{p}longitude></{p}locationForDisplay>\n'
    if s.get("road"): o += f'{sp}  <{p}roadNumber>{s["road"]}</{p}roadNumber>\n'
    if s.get("frm"):
        o += f'{sp}  <{p}linearExtension><{p}linearByCoordinatesExtension>\n'
        o += f'{sp}    <{p}linearCoordinatesStartPoint><{p}pointCoordinates><{p}latitude>{s["frm"][0]}</{p}latitude><{p}longitude>{s["frm"][1]}</{p}longitude></{p}pointCoordinates></{p}linearCoordinatesStartPoint>\n'
        o += f'{sp}    <{p}linearCoordinatesEndPoint><{p}pointCoordinates><{p}latitude>{s["to"][0]}</{p}latitude><{p}longitude>{s["to"][1]}</{p}longitude></{p}pointCoordinates></{p}linearCoordinatesEndPoint>\n'
        o += f'{sp}  </{p}linearByCoordinatesExtension></{p}linearExtension>\n'
    o += f'{sp}</{p}groupOfLocations>\n'
    return o

def rec_xml(s, p):
    o = f'    <{p}situation id="{s["sid"]}">\n'
    o += f'      <{p}situationRecord xsi:type="{p}{s["typ"]}" id="{s["rid"]}" version="1">\n'
    if s["comments"]:
        o += f'        <{p}generalPublicComment><{p}comment><{p}values>\n'
        for c in s["comments"]:
            o += f'          <{p}value lang="en">{escape(c)}</{p}value>\n'
        o += f'        </{p}values></{p}comment></{p}generalPublicComment>\n'
    else:
        o += f'        <{p}generalPublicComment/>\n'
    if s.get("sev"): o += f'        <{p}severity>{s["sev"]}</{p}severity>\n'
    if s.get("impact"): o += f'        <{p}trafficConstrictionType>{s["impact"]}</{p}trafficConstrictionType>\n'
    if s.get("mt"): o += f'        <{p}roadMaintenanceType>{s["mt"]}</{p}roadMaintenanceType>\n'
    o += f'        <{p}validity><{p}validityTimeSpecification>\n'
    o += f'          <{p}overallStartTime>{s["start"]}</{p}overallStartTime>\n'
    if s.get("end"): o += f'          <{p}overallEndTime>{s["end"]}</{p}overallEndTime>\n'
    o += f'        </{p}validityTimeSpecification></{p}validity>\n'
    o += loc_xml(s, p, 8)
    o += f'      </{p}situationRecord>\n    </{p}situation>\n'
    return o

def doc(items, p):
    ns = 'xmlns:d2="http://datex2.eu/schema/2/2_0"' if p else 'xmlns="http://datex2.eu/schema/2/2_0"'
    root = f'{p}d2LogicalModel'
    o = '<?xml version="1.0" encoding="UTF-8"?>\n'
    o += f'<{root} {ns} xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" modelBaseVersion="2">\n'
    o += f'  <{p}payloadPublication xsi:type="{p}SituationPublication" lang="en">\n'
    for s in items: o += rec_xml(s, p)
    o += f'  </{p}payloadPublication>\n</{root}>\n'
    return o

os.makedirs("fixtures", exist_ok=True)
open("fixtures/TD1_unplanned.xml", "w").write(doc(U, ""))        # default namespace
open("fixtures/TD1_roadworks.xml", "w").write(doc(R, "d2:"))     # prefixed namespace
trunc = doc(U[:1], "").replace("</d2LogicalModel>", "").replace("  </payloadPublication>\n", "")
open("fixtures/malformed.xml", "w").write(trunc + '    <situation id="SIT_BROKEN">\n      <situationRecord xsi:type="Accident" id="X">\n        <generalPublicComment><comment><values><value>Truncated feed...\n')
json.dump(dict(U=U, R=R), open("fixtures/td1.json", "w"), indent=1)
print("fixtures written:", len(U), "unplanned,", len(R), "roadworks")
