package com.music.spotui.data.recommendation

import com.music.spotui.data.entity.*
import org.junit.Assert.*
import org.junit.Test

class HomePolishPolicyTest {
    private fun album(n:Int,artist:String="Artist $n")=HomeItem.Album("Album $n","art","Album",artist)
    @Test fun preservesTitlesAndContentTypesWhileAvoidingRepeatedCards() {
        val rows=listOf(HomeSection("Made For listener",listOf(album(1),album(2))),HomeSection("Albums",listOf(album(1),album(3))))
        val result=HomePolishPolicy.refine(rows)
        assertEquals(listOf("Made For listener","Albums"),result.map{it.title})
        assertEquals(3,result.flatMap{it.items}.size)
        assertTrue(result.flatMap{it.items}.all{it is HomeItem.Album})
    }
    @Test fun oneArtistCannotCrowdOutOtherAvailableArtists() {
        val rows=listOf(HomeSection("Albums",listOf(album(1,"A"),album(2,"A"),album(3,"A"),album(4,"B"),album(5,"C"))))
        val first=HomePolishPolicy.refine(rows).single().items.take(3).filterIsInstance<HomeItem.Album>()
        assertEquals(setOf("A","B","C"),first.map{it.artists}.toSet())
    }
    @Test fun newUserSpotifyRowsStayPopulatedAndTheFeedIsBounded() {
        val rows=(1..10).map{HomeSection("Shelf $it",listOf(album(it)))}
        val result=HomePolishPolicy.refine(rows)
        assertEquals(4,result.size);assertTrue(result.all{it.items.isNotEmpty()})
    }
}
