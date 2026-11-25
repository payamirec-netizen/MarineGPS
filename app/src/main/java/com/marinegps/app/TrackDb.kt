package com.marinegps.app
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="tracks")
data class Track(@PrimaryKey(autoGenerate=true) val id:Long=0,val started:Long,val ended:Long?=null,val activeMillis:Long=0,val meters:Double=0.0,val maxSpeed:Double=0.0,val state:String="recording")
@Entity(tableName="track_points",foreignKeys=[ForeignKey(entity=Track::class,parentColumns=["id"],childColumns=["trackId"],onDelete=ForeignKey.CASCADE)],indices=[Index("trackId")])
data class TrackPoint(@PrimaryKey(autoGenerate=true) val id:Long=0,val trackId:Long,val time:Long,val lat:Double,val lon:Double,val accuracy:Float,val segment:Long)
@Dao interface TrackDao {
 @Query("SELECT * FROM tracks ORDER BY id DESC") fun observe():Flow<List<Track>>
 @Query("SELECT * FROM track_points WHERE trackId=:id ORDER BY id") fun points(id:Long):Flow<List<TrackPoint>>
 @Query("SELECT * FROM tracks WHERE ended IS NULL ORDER BY id DESC LIMIT 1") suspend fun active():Track?
 @Insert suspend fun insert(t:Track):Long
 @Insert suspend fun point(p:TrackPoint)
 @Update suspend fun update(t:Track)
}
@Database(entities=[Track::class,TrackPoint::class],version=1,exportSchema=false)
abstract class TrackDb:RoomDatabase(){
 abstract fun tracks():TrackDao
 companion object {
 @Volatile private var instance:TrackDb?=null
 fun get(c:Context):TrackDb=instance?:synchronized(this){instance?:Room.databaseBuilder(c.applicationContext,TrackDb::class.java,"marine_tracks.db").build().also{instance=it}}
 }
}
internal fun trackStep(lat:Double,lon:Double,nextLat:Double,nextLon:Double,seconds:Double,accuracy:Double):Double {
 if(!validCoordinates(lat,lon)||!validCoordinates(nextLat,nextLon)||seconds<=0||seconds>30||accuracy !in 0.0..30.0)return 0.0
 val d=targetMetrics(lat,lon,nextLat,nextLon).meters
 return if(d>=maxOf(3.0,accuracy*0.5)&&d/seconds<=60)d else 0.0
}
