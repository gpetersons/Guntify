package com.music.spotui.data.recommendation

import com.music.spotui.data.preferences.*
import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class ArtistTastePolicyTest {
    private val day = 86_400_000L
    private val now = 100 * day
    private fun play(name: String, age: Int, duration: Long = 30_000, source: String = "spotui") = ArtistListeningSample(now - age * day, name, duration, source)
    @Test fun lastWeekOutweighsOlderListeningAndRanksTopThree() {
        val samples = (1..3).map { play("Current", it) } + (10..15).map { play("Older", it) } + (1..3).map { play("Third", it + 15) } + (1..3).map { play("Fourth", it + 22) }
        assertEquals(listOf("Current", "Older", "Third"), ArtistTastePolicy.topThree(samples, now))
    }
    @Test fun unqualifiedImportedExpiredAndOneDaySpamDoNotRelabelRows() {
        val samples = (1..20).map { play("Spam", 1) } + (1..3).map { play("Spotify", it, source = "spotify") } + (1..3).map { play("Short", it, 10_000) } + (31..35).map { play("Expired", it) }
        assertTrue(ArtistTastePolicy.topThree(samples, now).isEmpty())
    }
    @Test fun artistNamesWithAmpersandsArePreservedAndCaseDeduplicated() {
        assertEquals(listOf("Of Mice & Men"), ArtistTastePolicy.topThree(listOf(play("Of Mice & Men", 1), play("of mice & men", 2), play("Of Mice & Men", 3)), now))
    }
    private fun row(name: String) = HomeSection("More like $name", listOf(HomeItem.Playlist("$name 1", "", "", "$name-1"), HomeItem.Playlist("$name 2", "", "", "$name-2")))
    @Test fun rotationKeepsTitleAndPlaylistsTogetherAndLeavesEditorialAlone() {
        val editorial = HomeSection("New releases", emptyList())
        val feed = HomeFeedModel(sections = listOf(row("Eli"), editorial))
        val picks = listOf(row("A"), row("B"), row("C"))
        for (i in 0L..2L) {
            val output = ArtistTastePolicy.rotateRows(feed, picks, i)
            assertEquals(picks[i.toInt()], output.sections[0])
            assertSame(editorial, output.sections[1])
        }
    }
    @Test fun unavailableRelevantPlaylistsKeepOriginalRow() {
        val feed = HomeFeedModel(sections = listOf(row("Eli")))
        assertSame(feed, ArtistTastePolicy.rotateRows(feed, emptyList(), 1))
    }
    @Test fun keywordOnlyAndWeakPlaylistMatchesAreRejected() {
        assertFalse(ArtistTastePolicy.relevantPlaylist(0, 50, true))
        assertFalse(ArtistTastePolicy.relevantPlaylist(2, 50, false))
        assertTrue(ArtistTastePolicy.relevantPlaylist(8, 50, false))
        assertFalse(ArtistTastePolicy.relevantPlaylist(1, 20, true))
        assertTrue(ArtistTastePolicy.relevantPlaylist(1, 5, true))
    }
}
