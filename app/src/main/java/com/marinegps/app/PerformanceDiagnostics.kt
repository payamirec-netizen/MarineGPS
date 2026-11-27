package com.marinegps.app
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
@Composable internal fun PerformanceDiagnostics(){
 val c=LocalContext.current;val ar=LocalArabic.current
 val h by TrackHealth.state.collectAsState()
 val a by AnchorMonitor.state.collectAsState()
 val p=c.getSharedPreferences("marine_diagnostics",0)
 var open by remember{mutableStateOf(false)}
 TextButton(onClick={open=!open}){Text(if(ar)"تشخيص الأداء المحلي" else "Local diagnostics")}
 if(open){val rt=Runtime.getRuntime();Text(ltrValue("Heap: "+((rt.totalMemory()-rt.freeMemory())/1048576)+" MB"));Text((if(ar)"التسجيل: " else "Recording: ")+tr(h.message));Text((if(ar)"الرسو: " else "Anchor: ")+tr(a.status));Text((if(ar)"آخر خطأ: " else "Last error: ")+p.getString("lastError","—"));val time=p.getLong("lastErrorTime",0);if(time>0)Text(localDate(time))}
}
