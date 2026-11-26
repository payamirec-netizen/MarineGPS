package com.marinegps.app
import org.json.JSONObject
import org.json.JSONArray
internal fun backup(d:TransferData):String {
 fun p(v:TransferPoint)=JSONObject().put("name",v.name).put("lat",v.lat).put("lon",v.lon).put("notes",v.notes).put("time",v.time?:JSONObject.NULL).put("category",v.category).put("accuracy",v.accuracy?:JSONObject.NULL)
 return JSONObject().put("version",1).put("waypoints",JSONArray(d.points.map{p(it)})).put("tracks",JSONArray(d.tracks.map{t->JSONObject().put("name",t.name).put("started",t.started?:JSONObject.NULL).put("ended",t.ended?:JSONObject.NULL).put("activeMillis",t.activeMillis).put("meters",t.meters?:JSONObject.NULL).put("maxSpeed",t.maxSpeed).put("segments",JSONArray(t.segments.map{s->JSONArray(s.map{p(it)})}))})).put("settings",JSONObject(d.settings)).toString()
}
internal fun restore(raw:String):TransferData{
 require(raw.length<=8_000_000)
 val j=JSONObject(raw);require(j.getInt("version")==1)
 fun <T> list(a:JSONArray,f:(JSONObject)->T)= (0 until a.length()).map{f(a.getJSONObject(it))}
 fun time(o:JSONObject,k:String)=if(o.isNull(k))null else o.getLong(k).also{require(it>=0)}
 fun p(o:JSONObject):TransferPoint{val lat=o.getDouble("lat");val lon=o.getDouble("lon");require(validCoordinates(lat,lon));val acc=if(o.isNull("accuracy"))null else o.getDouble("accuracy").also{require(it.isFinite()&&it in 0.0..100000.0)}.toFloat();return TransferPoint(o.getString("name").take(200),lat,lon,o.optString("notes").take(4000),time(o,"time"),o.optString("category","Other").take(100),acc)}
 val tracks=list(j.getJSONArray("tracks")){o->val a=o.getJSONArray("segments");val ms=o.optLong("activeMillis");val meters=if(o.isNull("meters"))null else o.getDouble("meters");val speed=o.optDouble("maxSpeed",0.0);require(ms>=0&&(meters==null||meters.isFinite()&&meters>=0)&&speed.isFinite()&&speed>=0);require(time(o,"started")==null||time(o,"ended")==null||time(o,"ended")!!>=time(o,"started")!!);TransferTrack(o.getString("name").take(200),(0 until a.length()).map{list(a.getJSONArray(it),::p)},time(o,"started"),time(o,"ended"),ms,meters,speed)}
 val settings=j.optJSONObject("settings");val allowed=mapOf("speed" to listOf("kn","km/h","m/s"),"distance" to listOf("NM","km","mi"),"appearance" to listOf("System","Light","Dark"),"coordinates" to listOf("DD","DDM","DMS"),"language" to listOf("system","ar","en"))
 return TransferData(list(j.getJSONArray("waypoints"),::p),tracks,allowed.mapNotNull{(k,v)->settings?.optString(k)?.takeIf{it in v}?.let{k to it}}.toMap())
}
