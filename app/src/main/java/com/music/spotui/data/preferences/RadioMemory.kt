package com.music.spotui.data.preferences
import android.content.Context
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.recommendation.RadioMemoryPolicy
import org.json.JSONArray
import org.json.JSONObject

/** Bounded station memory in an existing exported preference store; never synchronized to Spotify. */
object RadioMemory {
    private const val RECENT="recent_v1"
    private const val QUEUE="queue_v1:"
    const val MAX_QUEUES=32
    const val MAX_QUEUE_BYTES=2*1024*1024
    data class Queue(val songs:List<SongsModel>,val shuffled:Boolean,val mode:String,val manualQueued:Set<Int> = emptySet())
    private fun prefs(c:Context)=c.getSharedPreferences("radio_progress",0)
    private fun records(c:Context)=runCatching {
        val a=JSONArray(prefs(c).getString(RECENT,"[]"))
        (0 until a.length()).map{val o=a.getJSONObject(it);RadioMemoryPolicy.Heard(o.optString("s"),o.optString("t"),o.optLong("at"))}
    }.getOrDefault(emptyList())
    private fun write(c:Context,rows:List<RadioMemoryPolicy.Heard>){prefs(c).edit().putString(RECENT,JSONArray().also{a->rows.forEach{a.put(JSONObject().put("s",it.station).put("t",it.track).put("at",it.at))}}.toString()).apply()}
    @Synchronized fun heard(c:Context,station:String,song:SongsModel,now:Long=System.currentTimeMillis()) {
        write(c,RadioMemoryPolicy.prune(records(c)+RadioMemoryPolicy.Heard(station,com.music.spotui.data.recommendation.RadioMixPolicy.key(song),now),now))
    }
    @Synchronized fun recent(c:Context,station:String,now:Long=System.currentTimeMillis()):Map<String,Long> {
        val old=records(c);val rows=RadioMemoryPolicy.prune(old,now)
        if(rows!=old)write(c,rows)
        cleanQueues(c,now)
        return rows.filter{it.station==station}.associate{it.track to it.at}
    }
    private fun encode(s:SongsModel)=JSONObject().put("id",s.id).put("title",s.title).put("album",s.album).put("singer",s.singer).put("cover",s.coverUri).put("url",s.url).put("spotify",s.spotifyTrackId).put("explicit",s.explicit).put("duration",s.durationMs)
    private fun decode(o:JSONObject)=SongsModel(o.optInt("id"),o.optString("title"),o.optString("album"),o.optString("singer"),o.optString("cover"),o.optString("url"),o.optString("spotify"),o.optBoolean("explicit"),o.optInt("duration"))
    @Synchronized fun saveQueue(c:Context,station:String,songs:List<SongsModel>,currentId:Int,shuffled:Boolean,now:Long=System.currentTimeMillis(),manualQueued:Set<Int> = emptySet()) {
        if(station.isBlank() || songs.isEmpty())return
        val index=songs.indexOfFirst{it.id==currentId}
        if(index<0)return
        val queue=songs.drop(index).distinctBy{it.id}.take(11)
        val value=JSONObject().put("manualQueued",JSONArray().also { a -> queue.filter { it.id in manualQueued }.forEach { a.put(it.id) } }).put("at",now).put("shuffle",shuffled).put("mode",RadioTaste.mode(c,station)).put("songs",JSONArray().also{a->queue.forEach{a.put(encode(it))}})
        prefs(c).edit().putString(QUEUE+station,value.toString()).apply();cleanQueues(c,now)
    }
    @Synchronized fun queue(c:Context,station:String,now:Long=System.currentTimeMillis()):Queue? {
        cleanQueues(c,now)
        return runCatching {
            val o=JSONObject(prefs(c).getString(QUEUE+station,null)?:return@runCatching null)
            val a=o.getJSONArray("songs");val songs=(0 until minOf(11,a.length())).map{decode(a.getJSONObject(it))}
            if(songs.isEmpty())null else Queue(songs,o.optBoolean("shuffle"),o.optString("mode","balanced"),o.optJSONArray("manualQueued")?.let { a -> (0 until a.length()).map { a.optInt(it) }.toSet() }.orEmpty())
        }.getOrNull()
    }
    private fun cleanQueues(c:Context,now:Long) {
        val p=prefs(c);val edit=p.edit()
        val valid=p.all.filterKeys{it.startsWith(QUEUE)}.mapNotNull{(key,value)->
            val raw=value as? String
            val at=runCatching{JSONObject(raw?:"").optLong("at")}.getOrNull()
            if(raw==null || at==null || at>now || now-at>=RadioMemoryPolicy.DAYS_90){edit.remove(key);null}else Triple(key,at,raw.toByteArray(Charsets.UTF_8).size)
        }.sortedByDescending{it.second}
        var bytes=0;var kept=0
        valid.forEach { row ->
            if(kept>=MAX_QUEUES || bytes+row.third>MAX_QUEUE_BYTES)edit.remove(row.first)
            else {bytes+=row.third;kept++}
        }
        edit.apply()
    }
}
