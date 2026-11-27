package com.marinegps.app
import org.json.JSONArray
internal fun mergeCatchEntries(old:String,incoming:String):String{
 val merged=JSONArray();val ids=mutableSetOf<String>()
 for(a in listOf(validateCatchEntries(JSONArray(old)),validateCatchEntries(JSONArray(incoming))))for(i in 0 until a.length()){val v=a.getJSONObject(i);if(ids.add(v.getString("id")))merged.put(v)}
 return validateCatchEntries(merged).toString()
}
