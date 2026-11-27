package com.marinegps.app
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray
import org.json.JSONObject
@Composable internal fun AnchorRange(distance:Double,radius:Int){
 val ar=LocalArabic.current
 Text(if(ar)"نطاق الرسو (المسافة فقط)" else "Anchor range (distance only)")
 Canvas(Modifier.fillMaxWidth().height(120.dp)){val scale=size.minDimension*.4f/maxOf(radius.toDouble(),distance);drawCircle(Color.Gray,(radius*scale).toFloat(),style=Stroke(2.dp.toPx()));drawCircle(if(distance>=radius)Color.Red else Color(0xff55c9ad),(distance*scale).toFloat(),style=Stroke(3.dp.toPx()));drawCircle(Color.Gray,3.dp.toPx())}
}
@Composable internal fun CatchLog(fix:Location?){
 val c=LocalContext.current;val ar=LocalArabic.current
 fun l(a:String,b:String)=if(ar)a else b
 val prefs=remember{c.getSharedPreferences("marine_fishing",0)}
 val loaded=remember{runCatching{val a=validateCatchEntries(JSONArray(prefs.getString("entries","[]")));(0 until a.length()).map{a.getJSONObject(it)}}}
 if(loaded.isFailure){Text(l("تعذر قراءة سجل المصيد. لم يتم حذف البيانات.","Cannot read catch log. Data has been preserved."),color=MaterialTheme.colorScheme.error);return}
 var records by remember{mutableStateOf(loaded.getOrThrow())}
 var species by remember{mutableStateOf("")};var count by remember{mutableStateOf("1")};var weight by remember{mutableStateOf("")};var bait by remember{mutableStateOf("")};var note by remember{mutableStateOf("")};var editing by remember{mutableStateOf<String?>(null)};var deleting by remember{mutableStateOf<String?>(null)}
 fun save(v:List<JSONObject>){prefs.edit().putString("entries",JSONArray(v).toString()).apply();records=v}
 Text(l("سجل المصيد","Catch log"),style=MaterialTheme.typography.titleMedium)
 OutlinedTextField(species,{species=it.take(100)},label={Text(l("نوع السمكة","Species"))},modifier=Modifier.fillMaxWidth())
 Row{OutlinedTextField(count,{count=it.take(6)},label={Text(l("العدد","Count"))},modifier=Modifier.weight(1f));OutlinedTextField(weight,{weight=it.take(12)},label={Text(l("الوزن kg","Weight kg"))},modifier=Modifier.weight(1f))}
 OutlinedTextField(bait,{bait=it.take(100)},label={Text(l("الطعم","Bait"))},modifier=Modifier.fillMaxWidth())
 OutlinedTextField(note,{note=it.take(500)},label={Text(l("ملاحظات","Notes"))},modifier=Modifier.fillMaxWidth())
 Button(enabled=species.isNotBlank()&&(count.toIntOrNull()?:0)>0&&(weight.isBlank()||weight.toDoubleOrNull()?.let{it.isFinite()&&it>=0}==true),onClick={val old=records.firstOrNull{it.optString("id")==editing};val v=JSONObject().put("id",editing?:java.util.UUID.randomUUID().toString()).put("time",old?.optLong("time")?:System.currentTimeMillis()).put("species",species).put("count",count.toInt()).put("weight",weight).put("bait",bait).put("note",note);if(old!=null){if(old.has("lat"))v.put("lat",old.getDouble("lat")).put("lon",old.getDouble("lon"))}else fix?.let{v.put("lat",it.latitude).put("lon",it.longitude)};save(listOf(v)+records.filterNot{it.optString("id")==v.optString("id")});editing=null;species="";count="1";weight="";bait="";note=""}){Text(l("حفظ المصيد","Save catch"))}
 if(editing!=null)TextButton(onClick={editing=null;species="";count="1";weight="";bait="";note=""}){Text(l("إلغاء التعديل","Cancel edit"))}
 TextButton(enabled=records.isNotEmpty(),onClick={val i=android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/json").putExtra(android.content.Intent.EXTRA_TEXT,JSONArray(records).toString());c.startActivity(android.content.Intent.createChooser(i,null))}){Text(l("مشاركة السجل JSON","Share log JSON"))}
 records.forEach{v->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(8.dp)){Text(v.optString("species")+" • "+v.optInt("count"));Text(localDate(v.optLong("time")));Text(v.optString("weight")+" kg • "+v.optString("bait"));Text(v.optString("note"));if(v.has("lat"))Text(ltrValue(v.optString("lat")+", "+v.optString("lon")));Row{TextButton(onClick={editing=v.optString("id");species=v.optString("species");count=v.optInt("count").toString();weight=v.optString("weight");bait=v.optString("bait");note=v.optString("note")}){Text(l("تعديل","Edit"))};TextButton(onClick={deleting=v.optString("id")}){Text(l("حذف","Delete"))}}}}}
 if(deleting!=null)AlertDialog(onDismissRequest={deleting=null},text={Text(l("حذف سجل المصيد؟","Delete catch?"))},confirmButton={TextButton(onClick={save(records.filterNot{it.optString("id")==deleting});deleting=null}){Text(l("حذف","Delete"))}},dismissButton={TextButton(onClick={deleting=null}){Text(l("إلغاء","Cancel"))}})
}
