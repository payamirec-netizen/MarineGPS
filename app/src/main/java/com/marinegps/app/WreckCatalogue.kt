package com.marinegps.app
import android.content.Context
internal fun loadWreckCatalogue(c:Context):List<Wreck>{
 val file=java.io.File(c.filesDir,"wrecks-ukho.geojson")
 if(file.exists())runCatching{android.util.AtomicFile(file).openRead().use{parseWrecks(readBounded(it).toString(Charsets.UTF_8))}}.getOrNull()?.takeIf{it.isNotEmpty()}?.let{return it}
 return c.assets.open("wrecks-ukho.geojson").use{parseWrecks(readBounded(it).toString(Charsets.UTF_8))}
}
