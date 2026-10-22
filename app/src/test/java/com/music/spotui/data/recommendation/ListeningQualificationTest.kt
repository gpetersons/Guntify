package com.music.spotui.data.recommendation

import com.music.spotui.audio.ListeningClock
import com.music.spotui.data.preferences.HistoryEntry
import com.music.spotui.data.preferences.ArtistListeningSample
import com.music.spotui.data.preferences.ArtistTastePolicy
import org.junit.Assert.*
import org.junit.Test

class ListeningQualificationTest {
    private val now = 100L * 86_400_000L
    private val durations = listOf(0L, 10_250L, 29_999L, 30_000L, 45_000L)
    @Test fun homeTasteUsesThirtySecondMinimum() {
        val samples = durations.map { LocalTasteSample("s$it", "Artist", now, it, "spotui") }
        assertEquals(listOf(30_000L,45_000L),LocalHomePolicy.qualifying(samples,now).map { it.listenedMs })
    }
    @Test fun statsAndSmartDownloadsUseSameMinimum() {
        val entries = durations.mapIndexed { i, ms -> HistoryEntry(now,i,"Song","Artist",url="q$i",spotifyTrackId="s$i",listenedMs=ms) }
        assertEquals(2,ListeningTastePolicy.topSongs(entries,now,20).size)
        val summary=ListeningStatsPolicy.summary(entries,now,30)
        assertEquals(2,summary.plays);assertEquals(75_000L,summary.recordedMs)
    }
    @Test fun artistSectionsIgnoreTenToTwentyNineSecondSkips() {
        val short=(0..2).map{ArtistListeningSample(now-it*86_400_000L,"Skipped",29_999,"spotui")}
        val long=(0..2).map{ArtistListeningSample(now-it*86_400_000L,"Listened",30_000,"spotui")}
        assertEquals(listOf("Listened"),ArtistTastePolicy.topThree(short+long,now))
    }
    @Test fun bufferingAndPauseTimeCannotMeetThirtySecondRequirement() {
        val clock=ListeningClock()
        for(t in 0L..20_000L step 250L) assertNull(clock.tick("visit",true,t))
        for(t in 20_250L..80_000L step 250L) assertNull(clock.tick("visit",false,t))
        for(t in 80_250L..90_000L step 250L) assertNull(clock.tick("visit",true,t))
        assertEquals(30_000L,clock.tick("visit",true,90_250L))
        assertNull(clock.tick("visit",true,90_500L))
    }
    @Test fun quickSkipsCannotCombineToQualifyAnotherSong() {
        val clock=ListeningClock()
        for(t in 0L..29_000L step 250L) assertNull(clock.tick("first",true,t))
        for(t in 29_250L..58_250L step 250L) assertNull(clock.tick("second",true,t))
    }
    @Test fun spotifyImportsRemainExcludedEvenAboveThirtySeconds() {
        val entry=HistoryEntry(now,1,"Song","Artist",url="q",listenedMs=60_000,source="spotify")
        assertTrue(ListeningTastePolicy.qualified(listOf(entry),now).isEmpty())
        assertTrue(LocalHomePolicy.qualifying(listOf(LocalTasteSample("s","Artist",now,60_000,"spotify")),now).isEmpty())
    }
}
