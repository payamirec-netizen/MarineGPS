package com.marinegps.app
import org.json.JSONObject
internal data class Wreck(val id:String,val name:String,val lat:Double,val lon:Double,val depth:Double?,val water:Double?,val status:String,val quality:String)
internal fun parseWrecks(raw:String):List<Wreck>{
 require(raw.length<=8000000)
 val arr=JSONObject(raw).getJSONArray("features")
 val out=mutableListOf<Wreck>()
 for(i in 0 until arr.length()){
 val f=arr.getJSONObject(i);val geo=f.optJSONObject("geometry")?:continue
 if(geo.optString("type")!="Point")continue
 val c=geo.optJSONArray("coordinates")?:continue;val lon=c.optDouble(0);val lat=c.optDouble(1)
 if(!validCoordinates(lat,lon)||lat !in 10.0..19.0||lon !in 40.0..56.0)continue
 val p=f.optJSONObject("properties")?:continue;val id=p.optString("wreck_id");if(id.isBlank()||id=="n/a")continue
 fun depth(k:String)=p.optString(k).toDoubleOrNull()?.takeIf{it.isFinite()&&it>0}
 out.add(Wreck(id,p.optString("name").takeUnless{it.isBlank()||it=="n/a"}?:"Wreck "+id,lat,lon,depth("depth"),depth("water_dept"),p.optString("status"),p.optString("position_quality",p.optString("pos_quality","Unknown"))))
 }
 return out.distinctBy{it.id}
}
internal fun nearbyWrecks(rows:List<Wreck>,lat:Double,lon:Double,radiusKm:Int)=rows.map{it to targetMetrics(lat,lon,it.lat,it.lon).meters}.filter{it.second<=radiusKm*1000.0}.sortedBy{it.second}
