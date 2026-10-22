package com.music.spotui.data.recommendation

import com.music.spotui.data.preferences.HomeSectionPolicy
import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class HomeSectionPolicyTest {
    @Test fun namedAndUnknownSectionsCannotReceiveUnrelatedDiscoveries() {
        listOf("More like Eli", "Because you listened to Eli", "Made for Example User", "Country", "Rock classics", "Eli Radio", "", "New releases").forEach {
            assertFalse(it, HomeSectionPolicy.canPersonalize(it))
        }
    }
    @Test fun genericSectionsRemainEligibleWithCaseAndWhitespaceVariations() {
        listOf("Your favorite artists", "Albums", "Playlists", "Recommended for you", " YOUR   TOP MIXES ").forEach {
            assertTrue(it, HomeSectionPolicy.canPersonalize(it))
        }
    }
    @Test fun seededSectionKeepsItemsAndOrderWhileGenericSectionCanChange() {
        val rock = HomeItem.Artist("Eli", "", "eli")
        val country = HomeItem.Artist("Country artist", "", "country")
        val named = HomeSection("More like Eli", listOf(rock))
        val feed = HomeFeedModel(sections = listOf(named, HomeSection("Artists", listOf(rock))))
        val output = HomeSectionPolicy.personalize(feed) { it.copy(title = "Wrong title", items = listOf(country)) }
        assertSame(named, output.sections[0])
        assertEquals("Artists", output.sections[1].title)
        assertEquals(listOf(country), output.sections[1].items)
    }
}
