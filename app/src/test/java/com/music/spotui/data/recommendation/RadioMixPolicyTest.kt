package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.*
import org.junit.Test
class RadioMixPolicyTest {
 private fun song(id:Int,artist:String="Artist")=SongsModel(id,"Song $id","Album",artist,"art","q$id","s$id",durationMs=180000)
 @Test fun fiveOriginsAllContributeBeforeASecondTurn(){val pools=(1..5).map{i->(1..5).map{song(i*10+it)}};assertEquals(listOf(11,21,31,41,51,12,22,32,42,52),RadioMixPolicy.blend(emptyList(),pools,5,"balanced").take(10).map{it.id})}
 @Test fun closeModeStillGivesAllFiveOriginalsAFirstTurn(){val pools=(1..5).map{i->(1..4).map{song(i*10+it)}};assertEquals(listOf(11,21,31,41,51),RadioMixPolicy.blend(emptyList(),pools,5,"close").take(5).map{it.id})}
 @Test fun duplicatesAndOriginalSongsAreRemovedAcrossLanes(){val out=RadioMixPolicy.blend(listOf("s1"),listOf(listOf(song(1),song(2),song(3)),listOf(song(2),song(4))),2,"balanced");assertEquals(listOf(2,4,3),out.map{it.id})}
 @Test fun emptyFailedLaneDoesNotDiscardOtherOrigins(){assertEquals(listOf(song(1),song(2)),RadioMixPolicy.blend(emptyList(),listOf(emptyList(),listOf(song(1),song(2)),emptyList()),3,"balanced"))}
 @Test fun fiveSeedLimitAndDistinctSelection(){assertEquals(5,RadioMixPolicy.selected((1..8).map(::song)+song(1)).size)}
 @Test fun closeAndExploreKeepEveryOriginalLane(){val pools=(0..2).map{i->(1..9).map{song(i*100+it)}};for(mode in listOf("close","explore","balanced")){val ids=RadioMixPolicy.blend(emptyList(),pools,2,mode).take(10).map{it.id};assertTrue(ids.any{it in 1..9});assertTrue(ids.any{it in 101..109});assertEquals(mode!="close",ids.any{it in 201..209})}}
 @Test fun exactRecordingCanMoveToNewCatalogId(){val old=song(1);val next=old.copy(id=2,spotifyTrackId="new");assertTrue(RadioSeedRepairPolicy.sameRecording(old,next,"ISRC","isrc"))}
 @Test fun titleAloneCannotRepairAnAnchor(){val old=song(1);assertFalse(RadioSeedRepairPolicy.sameRecording(old,old.copy(spotifyTrackId="new",singer="Different")))}
 @Test fun rejectsDifferentEditionExplicitnessAndDuration(){val old=song(1);assertFalse(RadioSeedRepairPolicy.sameRecording(old,old.copy(title="Song 1 Live",spotifyTrackId="new"),"ISRC","ISRC"));assertFalse(RadioSeedRepairPolicy.sameRecording(old,old.copy(explicit=true,spotifyTrackId="new"),"ISRC","ISRC"));assertFalse(RadioSeedRepairPolicy.sameRecording(old,old.copy(durationMs=200000,spotifyTrackId="new"),"ISRC","ISRC"))}
 @Test fun conflictingIsrcNeverFallsBackToTitleMatching(){val old=song(1);assertFalse(RadioSeedRepairPolicy.sameRecording(old,old.copy(spotifyTrackId="new"),"one","two"))}
 @Test fun missingIsrcRequiresAlbumAndKnownMatchingDuration(){val old=song(1);assertTrue(RadioSeedRepairPolicy.sameRecording(old,old.copy(spotifyTrackId="new")));assertFalse(RadioSeedRepairPolicy.sameRecording(old,old.copy(spotifyTrackId="new",durationMs=0)));assertFalse(RadioSeedRepairPolicy.sameRecording(old.copy(album=""),old.copy(spotifyTrackId="new")))}
 @Test fun closeNeverUsesEvolvingSongsEvenIfOriginalsRunOut(){assertEquals(listOf(1,2),RadioMixPolicy.blend(emptyList(),listOf(listOf(song(1),song(2)),(10..30).map(::song)),1,"close").map{it.id})}
 @Test fun balancedStartsThreeOriginalToOneEvolving(){val out=RadioMixPolicy.blend(emptyList(),listOf((1..12).map(::song),(101..112).map(::song)),1,"balanced");assertEquals(listOf(1,2,3,101,4,5,6,102),out.take(8).map{it.id})}
 @Test fun balancedDoesNotDrainAnEvolvingOnlyTail(){val out=RadioMixPolicy.blend(emptyList(),listOf(listOf(song(1)),(101..130).map(::song)),1,"balanced");assertEquals(listOf(1,101),out.map{it.id})}
 @Test fun exploreGivesMoreTurnsToNewInfluence(){assertEquals(listOf(1,101,102,103,2,104,105,106),RadioMixPolicy.blend(emptyList(),listOf((1..12).map(::song),(101..120).map(::song)),1,"explore").take(8).map{it.id})}
 @Test fun balancedDoesNotReplaceMissingOriginalsWithUnrelatedEvolvingLane(){assertTrue(RadioMixPolicy.blend(emptyList(),listOf(emptyList(),listOf(song(99))),1,"balanced").isEmpty())}
 @Test fun duplicateCandidatesDoNotStealAnOriginalsTurn(){val out=RadioMixPolicy.blend(emptyList(),listOf(listOf(song(1),song(1),song(2)),listOf(song(1),song(3),song(4))),2,"close");assertEquals(listOf(1,3,2,4),out.map{it.id})}
 @Test fun closeDisablesBroadFallbacksAndEvolution(){assertFalse(RadioDiscoveryPolicy.evolving("close"));assertFalse(RadioDiscoveryPolicy.artistFallback("close"));assertFalse(RadioDiscoveryPolicy.profileFallback("close"))}
 @Test fun balancedStopsBeforeProfileFallbackButExploreAndNonRadioKeepIt(){assertTrue(RadioDiscoveryPolicy.evolving("balanced"));assertTrue(RadioDiscoveryPolicy.artistFallback("balanced"));assertFalse(RadioDiscoveryPolicy.profileFallback("balanced"));assertTrue(RadioDiscoveryPolicy.profileFallback("explore"));assertTrue(RadioDiscoveryPolicy.profileFallback(null))}

}
