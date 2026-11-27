package com.marinegps.app
import org.json.JSONArray
internal fun validateCatchEntries(a:JSONArray):JSONArray{
 require(a.length()<=10000)
 val ids=mutableSetOf<String>()
 for(i in 0 until a.length()){
 val o=a.getJSONObject(i)
 require(o.getString("id").length in 1..100&&ids.add(o.getString("id")))
 require(o.getString("species").length in 1..100&&o.getInt("count")>0&&o.getLong("time")>=0)
 val weight=o.optString("weight");require(weight.isBlank()||weight.toDoubleOrNull()?.let{it.isFinite()&&it>=0}==true)
 require(o.optString("bait").length<=100&&o.optString("note").length<=500)
 if(o.has("lat")||o.has("lon"))require(validCoordinates(o.getDouble("lat"),o.getDouble("lon")))
 }
 return a
}
