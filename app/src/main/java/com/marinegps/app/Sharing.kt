package com.marinegps.app
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import com.google.zxing.MultiFormatWriter
import com.google.zxing.BarcodeFormat
internal fun shareFile(c:Context,ext:String,raw:String){
 require(ext in listOf("gpx","json","kml","csv"))
 val dir=java.io.File(c.cacheDir,"exports").apply{mkdirs()}
 val file=java.io.File(dir,"MarineGPS-"+System.currentTimeMillis()+"."+ext);file.writeText(raw)
 val uri=FileProvider.getUriForFile(c,c.packageName+".files",file)
 c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type=if(ext=="json")"application/json" else "application/gpx+xml";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Share MarineGPS"))
}
@Composable internal fun QrImage(p:TransferPoint){
 val bitmap=remember(p){val matrix=MultiFormatWriter().encode("geo:${p.lat},${p.lon}",BarcodeFormat.QR_CODE,512,512);Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888).apply{for(y in 0 until 512)for(x in 0 until 512)setPixel(x,y,if(matrix[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)}}
 Image(bitmap.asImageBitmap(),"Waypoint QR",Modifier.size(240.dp))
}
