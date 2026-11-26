package com.marinegps.app
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import java.io.ByteArrayInputStream

internal data class TransferPoint(val name:String,val lat:Double,val lon:Double,val notes:String="")
internal data class TransferTrack(val name:String,val segments:List<List<TransferPoint>>)
internal data class TransferData(val points:List<TransferPoint>,val tracks:List<TransferTrack>)
internal fun xml(s:String)=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
internal fun gpx(d:TransferData)=buildString {
 append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><gpx version=\"1.1\" creator=\"MarineGPS\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
 fun point(tag:String,p:TransferPoint){require(validCoordinates(p.lat,p.lon));append("<$tag lat=\"${p.lat}\" lon=\"${p.lon}\"><name>${xml(p.name)}</name><desc>${xml(p.notes)}</desc></$tag>")}
 d.points.forEach{point("wpt",it)}
 d.tracks.forEach{t->append("<trk><name>${xml(t.name)}</name>");t.segments.forEach{s->append("<trkseg>");s.forEach{point("trkpt",it)};append("</trkseg>")};append("</trk>")}
 append("</gpx>")
}
internal fun parseGpx(raw:String):TransferData {
 require(raw.length<=8_000_000&&!raw.contains("<!DOCTYPE",true)&&!raw.contains("<!ENTITY",true))
 val f=DocumentBuilderFactory.newInstance().apply{isNamespaceAware=true;isExpandEntityReferences=false}
 val root=f.newDocumentBuilder().parse(ByteArrayInputStream(raw.toByteArray(Charsets.UTF_8))).documentElement
 require(root.localName=="gpx")
 fun children(e:Element,n:String)= (0 until e.childNodes.length).mapNotNull{e.childNodes.item(it) as? Element}.filter{it.localName==n}
 fun value(e:Element,n:String)=children(e,n).firstOrNull()?.textContent.orEmpty()
 fun point(e:Element):TransferPoint {val lat=e.getAttribute("lat").toDouble();val lon=e.getAttribute("lon").toDouble();require(validCoordinates(lat,lon));return TransferPoint(value(e,"name").take(200),lat,lon,value(e,"desc").take(4000))}
 return TransferData(children(root,"wpt").map{point(it)},children(root,"trk").map{t->TransferTrack(value(t,"name"),children(t,"trkseg").map{s->children(s,"trkpt").map{point(it)}})})
}
internal fun csv(d:TransferData)=buildString {
 fun q(s:String)="\""+(if(s.firstOrNull() in listOf('=','+','-','@','\t','\r')) "'"+s else s).replace("\"","\"\"")+"\""
 append("type,name,latitude,longitude,notes,segment\n")
 d.points.forEach{append("waypoint,${q(it.name)},${it.lat},${it.lon},${q(it.notes)},\n")}
 d.tracks.forEach{t->t.segments.forEachIndexed{i,s->s.forEach{append("track,${q(t.name)},${it.lat},${it.lon},,${i+1}\n")}}}
}
internal fun kml(d:TransferData)=buildString{
 append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><kml xmlns=\"http://www.opengis.net/kml/2.2\"><Document>")
 d.points.forEach{append("<Placemark><name>${xml(it.name)}</name><description>${xml(it.notes)}</description><Point><coordinates>${it.lon},${it.lat}</coordinates></Point></Placemark>")}
 d.tracks.forEach{t->t.segments.forEach{s->append("<Placemark><name>${xml(t.name)}</name><LineString><coordinates>");s.forEach{append("${it.lon},${it.lat} ")};append("</coordinates></LineString></Placemark>")}}
 append("</Document></kml>")
}
