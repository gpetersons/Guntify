package com.music.spotui.data.recommendation
import com.music.spotui.audio.ManualQueuePolicy
import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.*
import org.junit.Test
class ManualQueuePolicyTest {
 private fun s(i:Int)=SongsModel(i,"Song $i","Album","Artist","art","q$i")
 @Test fun additionGoesAfterCurrentAndPreservesHistory(){assertEquals(listOf(1,2,9,3,4),ManualQueuePolicy.add((1..4).map(::s),2,emptySet(),s(9))!!.map{it.id})}
 @Test fun multipleAdditionsKeepUserOrder(){val a=ManualQueuePolicy.add((1..3).map(::s),1,emptySet(),s(9))!!;assertEquals(listOf(1,9,8,2,3),ManualQueuePolicy.add(a,1,setOf(9),s(8))!!.map{it.id})}
 @Test fun existingUpcomingTrackCanBePromoted(){assertEquals(listOf(1,3,2),ManualQueuePolicy.add((1..3).map(::s),1,emptySet(),s(3))!!.map{it.id})}
 @Test fun currentAndAlreadyManuallyQueuedTracksAreNotDuplicated(){val q=listOf(s(1),s(9),s(2));assertNull(ManualQueuePolicy.add(q,1,setOf(9),s(9)));assertNull(ManualQueuePolicy.add(q,1,setOf(9),s(1)))}
 @Test fun playedHistoryIsNotRemovedToRequeueAnOldTrack(){assertNull(ManualQueuePolicy.add((1..3).map(::s),2,emptySet(),s(1)))}
 @Test fun emptyQueueCanReceiveFirstTrack(){assertEquals(listOf(s(1)),ManualQueuePolicy.add(emptyList(),0,emptySet(),s(1)))}
 @Test fun radioRefreshRetainsPendingSongsAheadOfSuggestions(){val q=listOf(s(1),s(4),s(5));assertEquals(listOf(1,9,8,4,5),ManualQueuePolicy.protect(q,1,listOf(s(9),s(8))).map{it.id})}
 @Test fun protectionDoesNotDuplicateTracksAlreadyInProposedQueue(){assertEquals(listOf(1,9,2),ManualQueuePolicy.protect(listOf(s(1),s(2),s(9)),1,listOf(s(9))).map{it.id})}
}
