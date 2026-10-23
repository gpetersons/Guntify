package com.music.spotui.data.recommendation
import com.music.spotui.data.entity.LibraryEntry
import org.junit.Assert.*
import org.junit.Test
class CreatedPlaylistLedgerTest {
    private val own = CreatedPlaylistRecord("account:one", "abc123", "My playlist", pending=true)
    @Test fun restartAndRemoteOmissionRetainActualPlaylistId() {
        val records=CreatedPlaylistLedger.decode(CreatedPlaylistLedger.encode(listOf(own)))
        assertEquals(listOf(own),records)
        val refreshed=CreatedPlaylistLedger.reconcile(own.owner, records, emptyList())
        assertEquals("abc123",CreatedPlaylistLedger.merge(own.owner,refreshed,emptyList()).single().spotifyId)
        assertTrue(refreshed.single().pending)
    }
    @Test fun accountSwitchDoesNotShowOrModifyOtherAccountsPlaylists() {
        assertTrue(CreatedPlaylistLedger.merge("account:two",listOf(own),emptyList()).isEmpty())
        assertEquals(listOf(own),CreatedPlaylistLedger.reconcile("account:two",listOf(own),emptyList()))
    }
    @Test fun remoteConfirmationUpdatesMetadataAndRemovesPendingWithoutDuplicate() {
        val remote=listOf(LibraryEntry(own.id,"Updated name","Playlist","art",true))
        val confirmed=CreatedPlaylistLedger.reconcile(own.owner,listOf(own),remote)
        assertFalse(confirmed.single().pending);assertEquals("Updated name",confirmed.single().name)
        assertEquals(remote,CreatedPlaylistLedger.merge(own.owner,confirmed,remote))
    }
    @Test fun albumIdCollisionDoesNotHidePlaylist() {
        val remote=listOf(LibraryEntry(own.id,"Album","Album","",false))
        assertEquals(2,CreatedPlaylistLedger.merge(own.owner,listOf(own),remote).size)
    }
    @Test fun explicitRemovalIsNotReconstructedByAnEmptyRefresh() {
        val removed=listOf(own).filterNot{it.id==own.id && it.owner==own.owner}
        assertTrue(CreatedPlaylistLedger.merge(own.owner,CreatedPlaylistLedger.reconcile(own.owner,removed,emptyList()),emptyList()).isEmpty())
    }
    @Test fun samePlaylistCanBeRecordedSeparatelyPerAccount() {
        val all=listOf(own,own.copy(owner="account:two"))
        assertEquals(2,CreatedPlaylistLedger.decode(CreatedPlaylistLedger.encode(all)).size)
    }
}
