import csv,json,urllib.request,urllib.parse,math,pathlib,xml.etree.ElementTree as ET
out=pathlib.Path("wreck-results");out.mkdir(exist_ok=True)
report={}
def request(url,params):
    u=url+"?"+urllib.parse.urlencode(params)
    with urllib.request.urlopen(urllib.request.Request(u,headers={"User-Agent":"MarineGPS research/0.8"}),timeout=120) as f:return json.load(f)
def export(name,features):
    (out/(name+".geojson")).write_text(json.dumps({"type":"FeatureCollection","features":features},ensure_ascii=False,indent=2))
    rows=[];root=ET.Element("gpx",version="1.1",creator="MarineGPS research",xmlns="http://www.topografix.com/GPX/1/1")
    for f in features:
        geo=f.get("geometry") or {};coords=geo.get("coordinates",[])
        if geo.get("type")!="Point":continue
        lon,lat=coords[:2]
        if not (40<=lon<=56 and 10<=lat<=19):continue
        p=f.get("properties") or {};row={**p,"latitude_dd":lat,"longitude_dd":lon};rows.append(row)
        w=ET.SubElement(root,"wpt",lat=str(lat),lon=str(lon));ET.SubElement(w,"name").text=str(p.get("name") or p.get("wreck_id") or "Unnamed")
        ET.SubElement(w,"desc").text=json.dumps(p,ensure_ascii=False)
    keys=sorted(set().union(*(r.keys() for r in rows))) if rows else ["latitude","longitude"]
    with (out/(name+".csv")).open("w",encoding="utf-8-sig",newline="") as f:
        w=csv.DictWriter(f,keys);w.writeheader();w.writerows(rows)
    ET.ElementTree(root).write(out/(name+".gpx"),encoding="utf-8",xml_declaration=True)
    return {"features":len(features),"point_rows":len(rows),"aden_approx":sum(12.65<=r["latitude_dd"]<=13.0 and 44.75<=r["longitude_dd"]<=45.25 for r in rows),"sira_approx":sum(12.73<=r["latitude_dd"]<=12.81 and 45.0<=r["longitude_dd"]<=45.12 for r in rows)}
def emod():
    result=[];start=0
    while True:
        d=request("https://ows.emodnet-humanactivities.eu/wfs",{"service":"WFS","version":"2.0.0","request":"GetFeature","typeNames":"emodnet:wwshipwrecks","outputFormat":"application/json","srsName":"EPSG:4326","bbox":"40,10,56,19,urn:ogc:def:crs:OGC::CRS84","count":1000,"startIndex":start})
        page=d["features"];result.extend(page)
        if len(page)<1000:break
        start+=len(page)
        if start>20000:raise RuntimeError("Pagination limit")
    return result
def ukho():
    result=[]
    for layer in (0,1):
        d=request(f"https://services-eu1.arcgis.com/OffW2AlaoR7SI7I9/arcgis/rest/services/Wrecks_and_Obstructions/FeatureServer/{layer}/query",{"where":"1=1","geometry":"40,10,56,19","geometryType":"esriGeometryEnvelope","inSR":4326,"outSR":4326,"spatialRel":"esriSpatialRelIntersects","outFields":"*","f":"geojson"})
        if "error" in d or d.get("exceededTransferLimit"):raise RuntimeError(str(d)[:500])
        result.extend(d["features"])
    return result
def osm():
    d=None
    for endpoint in ("https://overpass.kumi.systems/api/interpreter","https://overpass-api.de/api/interpreter"):
        try:
            d=request(endpoint,{"data":'[out:json][timeout:60];nwr["seamark:type"="wreck"](10,40,19,56);out center;'})
            break
        except Exception as e: last=e
    if d is None:raise last
    result=[]
    for e in d["elements"]:
        p=e.get("center",e)
        if "lat" in p:result.append({"type":"Feature","geometry":{"type":"Point","coordinates":[p["lon"],p["lat"]]},"properties":{"osm_id":str(e["type"])+"/"+str(e["id"]),**e.get("tags",{})}})
    return result
for name,fn in [("emodnet_ukho",emod),("ukho_direct",ukho),("osm",osm)]:
    try:report[name]={"status":"ok",**export(name,fn())}
    except Exception as e:report[name]={"status":"error","error":str(e)}
(out/"report.json").write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2))
