package com.marinegps.app
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
class CatchBackupTest{
 private fun entry()=JSONObject().put("id","a").put("species","Fish").put("count",1).put("time",1)
 @Test fun valid(){assertEquals(1,validateCatchEntries(JSONArray().put(entry())).length())}
 @Test(expected=IllegalArgumentException::class) fun duplicate(){validateCatchEntries(JSONArray().put(entry()).put(entry()))}
 @Test(expected=IllegalArgumentException::class) fun negativeCount(){validateCatchEntries(JSONArray().put(entry().put("count",-1)))}
}
