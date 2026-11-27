package com.marinegps.app
import android.content.Context
internal fun shouldPersistTrack(point:Boolean,newMax:Boolean,elapsed:Long)=point||newMax||elapsed>=5000
internal object LocalDiagnostics{
 fun record(c:Context,component:String,error:String){val p=c.getSharedPreferences("marine_diagnostics",0);p.edit().putString("lastError",component+": "+error).putLong("lastErrorTime",System.currentTimeMillis()).apply()}
}
