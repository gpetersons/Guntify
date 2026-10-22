package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.SongsModel
import kotlin.math.ln
import kotlin.random.Random

object RadioMemoryPolicy {
    const val DAYS_90=90L*86_400_000L
    const val PER_STATION=1000
    const val TOTAL=10000
    data class Heard(val station:String,val track:String,val at:Long)
    fun prune(records:List<Heard>,now:Long):List<Heard> {
        val counts=mutableMapOf<String,Int>()
        return records.filter{it.at<=now && now-it.at<DAYS_90 && it.station.isNotBlank() && it.track.isNotBlank()}
            .sortedByDescending{it.at}.distinctBy{it.station to it.track}
            .filter{val n=counts.getOrDefault(it.station,0);counts[it.station]=n+1;n<PER_STATION}.take(TOTAL)
    }
    /** Weighted random order: recent repeats remain possible, never a 24-hour ban. */
    fun order(songs:List<SongsModel>,heard:Map<String,Long>,now:Long,random:Random=Random.Default):List<SongsModel> =
        songs.distinctBy(RadioMixPolicy::key).map { song ->
            val at=heard[RadioMixPolicy.key(song)]
            val age=at?.let{now-it}
            val weight=when {age==null || age<0 || age>=DAYS_90->1.0;age<86_400_000L->0.12;age<7L*86_400_000L->0.4;else->0.85}
            song to -ln(random.nextDouble().coerceAtLeast(1e-12))/weight
        }.sortedBy{it.second}.map{it.first}
}
