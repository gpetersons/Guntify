package com.music.spotui.data.preferences

import android.content.Context
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.recommendation.RadioMixPolicy
import org.json.JSONArray
import org.json.JSONObject

object RadioSkipMemory {
    const val WINDOW_MS=7L*86_400_000L
    private fun prefs(c:Context)=c.getSharedPreferences("radio_skip_memory",0)
    fun enabled(c:Context)=prefs(c).getBoolean("enabled",true)
    fun setEnabled(c:Context,value:Boolean){prefs(c).edit().putBoolean("enabled",value).apply()}
    fun avoids(c:Context,song:SongsModel,now:Long=System.currentTimeMillis()):Boolean = enabled(c) &&
        runCatching { JSONObject(prefs(c).getString(RadioMixPolicy.key(song),"{}")).optLong("until")>now }.getOrDefault(false)
    @Synchronized fun skipped(c:Context,song:SongsModel,audibleMs:Long,visit:String,now:Long=System.currentTimeMillis()) {
        if(!enabled(c) || audibleMs<=0 || com.music.spotui.audio.ListeningQualification.qualifies(audibleMs) || visit.isBlank())return
        val p=prefs(c);val key=RadioMixPolicy.key(song)
        val old=runCatching{JSONObject(p.getString(key,"{}"))}.getOrDefault(JSONObject())
        if(old.optString("visit")==visit)return
        val times=old.optJSONArray("times")?:JSONArray()
        val kept=(0 until times.length()).map{times.optLong(it)}.filter{it<=now && now-it<WINDOW_MS}.takeLast(2)+now
        val next=JSONObject().put("visit",visit).put("times",JSONArray(kept)).put("updated",now)
            .put("until",if(kept.size>=3)now+WINDOW_MS else old.optLong("until"))
        p.edit().putString(key,next.toString()).apply()
        // Bound storage without keeping an unlimited log of skips.
        val stale=p.all.filter { (k,_) -> k!="enabled" }.mapNotNull { (k,v) ->
            runCatching{k to JSONObject(v as String).optLong("updated")}.getOrNull()
        }.sortedByDescending{it.second}.drop(500)
        if(stale.isNotEmpty()){val edit=p.edit();stale.forEach{edit.remove(it.first)};edit.apply()}
    }
    @Synchronized fun listened(c:Context,song:SongsModel){prefs(c).edit().remove(RadioMixPolicy.key(song)).apply()}
    fun clear(c:Context){val on=enabled(c);prefs(c).edit().clear().putBoolean("enabled",on).apply()}
}
