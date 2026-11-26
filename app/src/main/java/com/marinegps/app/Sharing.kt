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

internal fun parseGeo(raw:String):TransferPoint{
 require(raw.startsWith("geo:"));val parts=raw.substringAfter("geo:").substringBefore("?").split(",");require(parts.size==2)
 val lat=parts[0].toDouble();val lon=parts[1].toDouble();require(validCoordinates(lat,lon));return TransferPoint("Imported waypoint",lat,lon)
}
internal fun readQrData(c:Context,uri:android.net.Uri):TransferData{
 val options=android.graphics.BitmapFactory.Options().apply{inJustDecodeBounds=true}
 c.contentResolver.openInputStream(uri)?.use{android.graphics.BitmapFactory.decodeStream(it,null,options)}
 require(options.outWidth>0&&options.outHeight>0)
 var sample=1;while(maxOf(options.outWidth,options.outHeight)/sample>2048)sample*=2
 val bitmap=c.contentResolver.openInputStream(uri)?.use{android.graphics.BitmapFactory.decodeStream(it,null,android.graphics.BitmapFactory.Options().apply{inSampleSize=sample})}?:error("Invalid image")
 try{val pixels=IntArray(bitmap.width*bitmap.height);bitmap.getPixels(pixels,0,bitmap.width,0,0,bitmap.width,bitmap.height)
 val source=com.google.zxing.RGBLuminanceSource(bitmap.width,bitmap.height,pixels)
 return decodeQrPayload(com.google.zxing.MultiFormatReader().decode(com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))).text)
 }finally{bitmap.recycle()}
}

internal fun trackKey(segments:List<List<TransferPoint>>):String {val digest=java.security.MessageDigest.getInstance("SHA-256");segments.forEach{seg->digest.update(0.toByte());seg.forEach{p->digest.update(java.nio.ByteBuffer.allocate(16).putDouble(p.lat).putDouble(p.lon).array())}};return digest.digest().joinToString(""){ "%02x".format(it.toInt() and 255) }}

internal fun decodeQrPayload(raw:String):TransferData=if(raw.startsWith("MarineGPS:"))restore(raw.removePrefix("MarineGPS:")).also{require(it.tracks.isEmpty()&&it.points.isNotEmpty())}else TransferData(listOf(parseGeo(raw)),emptyList())
@Composable internal fun GroupQrImage(data:TransferData){
 val bitmap=remember(data){runCatching{val raw=if(data.points.size==1)"geo:${data.points.first().lat},${data.points.first().lon}" else "MarineGPS:"+backup(data);require(raw.toByteArray().size<=1800);val m=MultiFormatWriter().encode(raw,BarcodeFormat.QR_CODE,768,768,mapOf(com.google.zxing.EncodeHintType.CHARACTER_SET to "UTF-8"));Bitmap.createBitmap(768,768,Bitmap.Config.ARGB_8888).apply{for(y in 0 until 768)for(x in 0 until 768)setPixel(x,y,if(m[x,y])android.graphics.Color.BLACK else android.graphics.Color.WHITE)}}.getOrNull()}
 if(bitmap==null)androidx.compose.material3.Text(tr("QR too large; share a file instead"))else Image(bitmap.asImageBitmap(),"QR",Modifier.size(260.dp))
}
