package com.music.spotui.data.recommendation
import com.music.spotui.ui.notification.VoiceSearchPolicy
import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.*
import org.junit.Test
class VoiceSearchPolicyTest {
 private fun s(i:Int,title:String="Song",artist:String="Artist")=SongsModel(i,title,"Album",artist,"art","q$i")
 @Test fun titleAndArtistExtrasBuildCatalogQuery(){assertEquals("Song Artist",VoiceSearchPolicy.Request("raw", "Song", "Artist").query)}
 @Test fun rawQueryIsTrimmedAndBounded(){assertEquals("Song",VoiceSearchPolicy.Request(" Song ").query);assertEquals(200,VoiceSearchPolicy.Request("a".repeat(300)).query.length)}
 @Test fun emptyQueryIsGenericResume(){assertTrue(VoiceSearchPolicy.Request(" ").query.isBlank())}
 @Test fun exactTitleAndArtistWinOverSameTitleByOtherArtist(){val good=s(2);assertEquals(good,VoiceSearchPolicy.rank(VoiceSearchPolicy.Request("Song Artist","Song","Artist"),listOf(s(1,artist="Other"),good)).first())}
 @Test fun rawTitlePrefersExactOverAlternateVersion(){assertEquals(2,VoiceSearchPolicy.rank(VoiceSearchPolicy.Request("Song"),listOf(s(1,"Song remix"),s(2))).first().id)}
 @Test fun tiesKeepSpotifyOrderAndUnplayableDuplicatesAreRemoved(){val song=s(1);assertEquals(listOf(song,s(2)),VoiceSearchPolicy.rank(VoiceSearchPolicy.Request("x"),listOf(song,song,s(3).copy(url=""),s(2))))}
 @Test fun paginationHandlesInvalidAndOverflowingRequests(){val q=(1..5).map{ s(it) };assertEquals(q.drop(2).take(2),VoiceSearchPolicy.page(q,1,2));assertTrue(VoiceSearchPolicy.page(q,Int.MAX_VALUE,Int.MAX_VALUE).isEmpty());assertTrue(VoiceSearchPolicy.page(q,-1,4).isEmpty());assertTrue(VoiceSearchPolicy.page(q,0,0).isEmpty())}
 @Test fun partialExtrasKeepTheSpokenTitle(){assertEquals("Song Artist",VoiceSearchPolicy.Request("Song",artist="Artist").query)}
 @Test fun nonLatinTitlesCanBeMatchedExactly(){assertEquals(2,VoiceSearchPolicy.rank(VoiceSearchPolicy.Request("春"),listOf(s(1,"夏"),s(2,"春"))).first().id)}

}
