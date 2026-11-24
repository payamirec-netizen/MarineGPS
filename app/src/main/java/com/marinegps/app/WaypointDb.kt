package com.marinegps.app
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlin.math.*

@Entity(tableName="waypoints")
data class Waypoint(@PrimaryKey(autoGenerate=true) val id:Long=0,val name:String,val latitude:Double,val longitude:Double,val category:String,val notes:String,val createdAt:Long,val accuracy:Float?=null)
@Dao interface WaypointDao {
 @Query("SELECT * FROM waypoints ORDER BY createdAt DESC, id DESC") fun observe():Flow<List<Waypoint>>
 @Insert suspend fun insert(point:Waypoint)
 @Update suspend fun update(point:Waypoint)
 @Delete suspend fun delete(point:Waypoint)
}
@Database(entities=[Waypoint::class],version=1,exportSchema=false)
abstract class WaypointDb:RoomDatabase(){
 abstract fun points():WaypointDao
 companion object {
  @Volatile private var instance:WaypointDb?=null
  fun get(context:Context):WaypointDb=instance?:synchronized(this){instance?:Room.databaseBuilder(context.applicationContext,WaypointDb::class.java,"marine_waypoints.db").build().also{instance=it}}
 }
}
internal fun validCoordinates(lat:Double,lon:Double)=lat.isFinite()&&lon.isFinite()&&lat in -90.0..90.0&&lon in -180.0..180.0
internal fun freshFix(ageMillis:Long)=ageMillis in 0..5000
internal data class TargetMetrics(val meters:Double,val bearing:Double?)
internal fun targetMetrics(lat:Double,lon:Double,toLat:Double,toLon:Double):TargetMetrics{
 val a=Math.toRadians(lat);val b=Math.toRadians(toLat);val d=Math.toRadians(toLon-lon)
 val h=(sin((b-a)/2).pow(2)+cos(a)*cos(b)*sin(d/2).pow(2)).coerceIn(0.0,1.0)
 val m=6371008.8*2*asin(sqrt(h))
 val y=sin(d)*cos(b);val x=cos(a)*sin(b)-sin(a)*cos(b)*cos(d)
 return TargetMetrics(m,if(m<0.01||abs(x)+abs(y)<1e-12)null else (Math.toDegrees(atan2(y,x))+360)%360)
}
