package com.music.spotui.data.recommendation

import com.music.spotui.audio.ListeningClock
import com.music.spotui.audio.PlaybackClockPolicy
import com.music.spotui.data.entity.SongsModel
import org.junit.Assert.*
import org.junit.Test

class PlaybackBehaviorTest {
    @Test fun qualifiesAtThirtySeconds() {
        val clock = ListeningClock()
        for (t in 0L..29_750L step 250L) assertNull(clock.tick("song", true, t))
        assertNotNull(clock.tick("song", true, 30_000L))
    }
    @Test fun pausesAndBufferingNeverCount() {
        val clock = ListeningClock()
        for (t in 0L..20_000L step 250L) assertNull(clock.tick("song", false, t))
        for (t in 20_250L..50_000L step 250L) assertNull(clock.tick("song", true, t))
        assertNotNull(clock.tick("song", true, 50_250L))
    }
    @Test fun quickSkipsDoNotAccumulateAcrossSongs() {
        val clock = ListeningClock()
        for (t in 0L..9_000L step 250L) assertNull(clock.tick("a", true, t))
        for (t in 9_250L..18_250L step 250L) assertNull(clock.tick("b", true, t))
    }
    @Test fun aSuspendedPollingLoopCannotInventListeningTime() {
        val clock = ListeningClock()
        assertNull(clock.tick("song", true, 0))
        assertNull(clock.tick("song", true, 100_000))
    }
    @Test fun qualificationHappensOncePerPlaybackSession() {
        val clock = ListeningClock()
        var qualified = 0
        for (t in 0L..40_000L step 250L) if (clock.tick("song", true, t) != null) qualified++
        assertEquals(1, qualified)
    }
    @Test fun unknownAndAbsurdDurationsAreSafe() {
        assertEquals(0L, PlaybackClockPolicy.duration(Long.MIN_VALUE + 1))
        assertEquals(0L, PlaybackClockPolicy.duration(Long.MAX_VALUE))
        assertEquals(0L, PlaybackClockPolicy.position(-123, 250_000))
        assertEquals(0L, PlaybackClockPolicy.position(Long.MAX_VALUE, 250_000))
        assertEquals(0L, PlaybackClockPolicy.position(Long.MAX_VALUE, 0))
        assertEquals(250_000L, PlaybackClockPolicy.position(251_000, 250_000))
        assertEquals(180_000L, PlaybackClockPolicy.duration(180_000))
    }
    @Test fun shuffleStartDoesNotKeepTheDisplayedFirstTrack() {
        val tracks = (1..15).map { SongsModel(it, "song$it", "album", "artist", "", "", "id$it") }
        repeat(30) {
            val shuffled = RadioQueuePolicy.shuffled(tracks)
            assertNotEquals(tracks.first(), shuffled.first())
            assertEquals(tracks.toSet(), shuffled.toSet())
            assertEquals(15, shuffled.size)
        }
    }
}
