package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.SongsModel
import org.junit.Test
import org.junit.Assert.*
import kotlin.random.Random
class RadioMemoryPolicyTest {
 private fun song(i:Int)=SongsModel(i,"Song$i","Album","Artist","art","q$i","s$i")
 @Test fun retainsOnlyNinetyDaysAndRejectsFutureTimes(){val now=RadioMemoryPolicy.DAYS_90+100;assertEquals(listOf("new"),RadioMemoryPolicy.prune(listOf(RadioMemoryPolicy.Heard("a","old",100),RadioMemoryPolicy.Heard("a","new",101),RadioMemoryPolicy.Heard("a","future",now+1)),now).map{it.track})}
 @Test fun latestListenReplacesOldTimestampWithoutCombiningStations(){val rows=RadioMemoryPolicy.prune(listOf(RadioMemoryPolicy.Heard("a","song",1),RadioMemoryPolicy.Heard("a","song",3),RadioMemoryPolicy.Heard("b","song",2)),4);assertEquals(2,rows.size);assertEquals(3L,rows.first().at)}
 @Test fun stationCapKeepsNewestThousand(){val rows=RadioMemoryPolicy.prune((1..1200).map{RadioMemoryPolicy.Heard("a","s$it",it.toLong())},1300);assertEquals(1000,rows.size);assertEquals(201L,rows.last().at)}
 @Test fun globalCapKeepsNewestTenThousand(){val rows=RadioMemoryPolicy.prune((1..12000).map{RadioMemoryPolicy.Heard("station${it/900}","s$it",it.toLong())},13000);assertEquals(10000,rows.size);assertEquals(2001L,rows.last().at)}
 @Test fun recentSongsRemainPossibleButFreshSongsUsuallyComeFirst(){val now=10L*86400000;var fresh=0;var recent=0;repeat(1000){val first=RadioMemoryPolicy.order(listOf(song(1),song(2)),mapOf("s2" to now-1000),now,Random(it)).first().id;if(first==1)fresh++ else recent++};assertTrue(fresh>700);assertTrue(recent>20)}
 @Test fun samePoolCanProduceDifferentSequencesWithoutLosingTracks(){val songs=(1..10).map(::song);val a=RadioMemoryPolicy.order(songs,emptyMap(),100,Random(1));val b=RadioMemoryPolicy.order(songs,emptyMap(),100,Random(2));assertNotEquals(a,b);assertEquals(songs.toSet(),a.toSet());assertEquals(songs.toSet(),b.toSet())}
 @Test fun expiredHistoryDoesNotPenalizeAnOlderSong(){val now=RadioMemoryPolicy.DAYS_90+1;assertEquals(RadioMemoryPolicy.order(listOf(song(1),song(2)),emptyMap(),now,Random(1)),RadioMemoryPolicy.order(listOf(song(1),song(2)),mapOf("s2" to 1L),now,Random(1)))}
}
