package com.metrolist.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class PlaylistChangesTest {
    @Test fun addUsesPlaylistServiceWithActualTrackUrisAndNoDelete() = runBlocking {
        var requests = 0
        val client = HttpClient(MockEngine { request ->
            requests++
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("https://spclient.wg.spotify.com/playlist/v2/playlist/abc123/changes",request.url.toString())
            assertEquals("Bearer account-token",request.headers[HttpHeaders.Authorization])
            val body=Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            val op=body["deltas"]!!.jsonArray[0].jsonObject["ops"]!!.jsonArray.single().jsonObject
            assertEquals("ADD",op["kind"]!!.jsonPrimitive.content)
            assertEquals("spotify:track:song1",op["add"]!!.jsonObject["items"]!!.jsonArray[0].jsonObject["uri"]!!.jsonPrimitive.content)
            respond("{\"revision\":\"next\"}",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        })
        try { PlaylistChanges(client).add("abc123",listOf("spotify:track:song1"),"account-token");assertEquals(1,requests) } finally {client.close()}
    }
    @Test fun removalTargetsOneOccurrenceByUid() = runBlocking {
        val client=HttpClient(MockEngine { request ->
            assertEquals(HttpMethod.Post,request.method)
            val op=Json.parseToJsonElement((request.body as TextContent).text).jsonObject["deltas"]!!.jsonArray[0].jsonObject["ops"]!!.jsonArray.single().jsonObject
            assertEquals("REM",op["kind"]!!.jsonPrimitive.content)
            val item=op["rem"]!!.jsonObject["items"]!!.jsonArray.single().jsonObject
            assertEquals("second-occurrence",item["uid"]!!.jsonPrimitive.content)
            assertEquals("spotify:track:song1",item["uri"]!!.jsonPrimitive.content)
            respond("{}",HttpStatusCode.OK)
        })
        try { PlaylistChanges(client).remove("abc123",listOf(Spotify.PlaylistItemRef("spotify:track:song1","second-occurrence")),"token") } finally {client.close()}
    }
    @Test fun rateLimitDoesNotRetryOrSwitchToAnotherWriteEndpoint() = runBlocking {
        var requests=0
        val client=HttpClient(MockEngine { requests++;respond("{}",HttpStatusCode.TooManyRequests,headersOf("Retry-After","15")) })
        try {
            try { PlaylistChanges(client).add("abc123",listOf("spotify:track:song1"),"token");fail("Failure was hidden") }
            catch(e:Spotify.SpotifyException){assertEquals(429,e.statusCode);assertEquals(15L,e.retryAfterSec)}
            assertEquals(1,requests)
        }finally{client.close()}
    }
    @Test fun embeddedErrorIsNeverReportedAsSuccessfulWrite() = runBlocking {
        val client=HttpClient(MockEngine { respond("{\"error\":{\"message\":\"not allowed\"}}",HttpStatusCode.OK) })
        try { try{PlaylistChanges(client).add("abc123",listOf("spotify:track:song1"),"token");fail("False success")}catch(_:Spotify.SpotifyException){} }finally{client.close()}
    }
    @Test fun missingUidCannotRemoveEveryCopyOfASong() = runBlocking {
        var requests=0;val client=HttpClient(MockEngine{requests++;respond("{}")})
        try{try{PlaylistChanges(client).remove("abc123",listOf(Spotify.PlaylistItemRef("spotify:track:song1","")),"token");fail("Missing uid accepted")}catch(_:IllegalArgumentException){};assertEquals(0,requests)}finally{client.close()}
    }
    @Test fun expiredLibraryHashStillHasAnExplicitFallback() {
        assertEquals("7c5a69420e2bfae3da5cc4e14cbc8bb3f6090f80afc00ffc179177f19be3f33d",SpotifyHashProvider.getHash("removeFromLibrary"))
        assertNotNull(SpotifyHashProvider.getPreviousHash("removeFromLibrary"))
    }
}
