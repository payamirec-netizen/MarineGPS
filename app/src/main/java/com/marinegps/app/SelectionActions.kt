package com.marinegps.app
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
@Composable internal fun SelectionActions(data:TransferData,delete:()->Unit){
 val c=LocalContext.current;val scope=rememberCoroutineScope()
 var mode by remember{mutableStateOf("")};var raw by remember{mutableStateOf("")};var error by remember{mutableStateOf("")}
 val save=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->if(uri!=null)scope.launch{try{withContext(Dispatchers.IO){c.contentResolver.openOutputStream(uri)?.use{it.write(raw.toByteArray())}?:error("Cannot write file")}}catch(e:Exception){error=e.message.orEmpty()}}}
 Column{Text(tr("Selected")+" • "+(data.points.size+data.tracks.size));Row{TextButton(onClick={mode="Share"}){Text(tr("Share"))};TextButton(onClick={mode="Export"}){Text(tr("Export"))};TextButton(onClick=delete){Text(tr("Delete"),color=MaterialTheme.colorScheme.error)}}
 if(data.points.isNotEmpty()&&data.tracks.isEmpty()){Row{TextButton(onClick={mode="QR"}){Text("QR")};TextButton(onClick={c.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText("Coordinates",data.points.joinToString("\n"){"${it.lat}, ${it.lon}"}))}){Text(tr("Copy"))}}}
 if(error.isNotEmpty())Text(tr(error))
 }
 if(mode=="QR")AlertDialog(onDismissRequest={mode=""},title={Text("QR")},text={GroupQrImage(data)},confirmButton={TextButton(onClick={mode=""}){Text(tr("Close"))}})
 else if(mode.isNotEmpty())AlertDialog(onDismissRequest={mode=""},title={Text(tr(mode))},text={Column{listOf("gpx","kml","csv","json").forEach{ext->TextButton(onClick={val share=mode=="Share";mode="";scope.launch{try{raw=withContext(Dispatchers.Default){when(ext){"gpx"->gpx(data);"kml"->kml(data);"csv"->csv(data);else->backup(data)}};if(share)shareFile(c,ext,raw)else save.launch("MarineGPS-selected."+ext)}catch(e:Exception){error=e.message.orEmpty()}}}){Text(ext.uppercase())}}}},confirmButton={TextButton(onClick={mode=""}){Text(tr("Cancel"))}})
}
