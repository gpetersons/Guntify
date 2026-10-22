package com.music.spotui.audio

/** A Play request can arrive after BUFFERING; duplicate events must not reset its deadline. */
class BufferingWatchPolicy {
    enum class Decision { START, KEEP, STOP }
    private var watching:Pair<Long,Int>?=null
    fun reset(){watching=null}
    fun observe(visit:Long,buffering:Boolean,wantsPlay:Boolean,playerIdentity:Int=0):Decision {
        if(!buffering || !wantsPlay){watching=null;return Decision.STOP}
        if(watching==(visit to playerIdentity))return Decision.KEEP
        watching=visit to playerIdentity
        return Decision.START
    }
}
