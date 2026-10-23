package com.metrolist.spotify

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlaylistLibraryTest {
    @Before fun readySession() { SpotifyWebSession.capture("https://spclient.wg.spotify.com/a", mapOf("client-token" to "a".repeat(40))) }
    @After fun cleanup() = SpotifyWebSession.clear()
    private fun snapshot(uris: List<String>, rev: String="rev", pos: Int=0, truncated: Boolean=false, total: Int=uris.size): String = buildJsonObject {
        put("revision", rev);put("length", total)
        putJsonObject("contents") { put("pos",pos);put("truncated",truncated);putJsonArray("items") { uris.forEach { uri -> add(buildJsonObject {put("uri",uri)}) } } }
    }.toString()
    @Test fun saveThenUnfollowTouchOnlyLibraryReferencesAndVerifyMembership() = runBlocking {
        val uri="spotify:playlist:abc123"
        val folder=listOf("spotify:start-group:folder","spotify:playlist:other","spotify:end-group:folder")
        var stored=folder;var writes=0
        val client=HttpClient(MockEngine { req ->
            assertTrue(req.url.encodedPath.startsWith("/playlist/v2/user/person/rootlist"))
            assertFalse(req.url.encodedPath.contains("/playlist/abc123/changes"))
            assertEquals("a".repeat(40),req.headers["client-token"])
            if(req.method==HttpMethod.Get) respond(snapshot(stored),HttpStatusCode.OK) else {
                writes++
                val body=Json.parseToJsonElement((req.body as TextContent).text).jsonObject
                assertEquals("rev",body["baseRevision"]!!.jsonPrimitive.content)
                val op=body["deltas"]!!.jsonArray.single().jsonObject["ops"]!!.jsonArray.single().jsonObject
                val add=op["kind"]!!.jsonPrimitive.content=="ADD"
                val change=op[if(add)"add"else"rem"]!!.jsonObject
                assertTrue(change[if(add)"addFirst"else"itemsAsKey"]!!.jsonPrimitive.boolean)
                assertEquals(uri,change["items"]!!.jsonArray.single().jsonObject["uri"]!!.jsonPrimitive.content)
                assertFalse(change.containsKey("fromIndex"))
                stored=if(add)listOf(uri)+folder else folder
                respond("{\"revision\":\"next\"}",HttpStatusCode.OK)
            }
        })
        try {
            val library=PlaylistLibrary(client)
            library.setSaved("person",listOf(uri),true,"token")
            library.setSaved("person",listOf(uri),true,"token") // existing membership is a no-op
            assertEquals(1,writes)
            library.setSaved("person",listOf(uri),false,"token")
            assertEquals(2,writes);assertEquals(folder,stored)
        } finally {client.close()}
    }
    @Test fun rateLimitHasNoWriteRetryAndCooldownPreventsAnotherRequest() = runBlocking {
        var requests=0
        val client=HttpClient(MockEngine {requests++;respond("{}",HttpStatusCode.TooManyRequests,headersOf("Retry-After","60"))})
        try {
            val library=PlaylistLibrary(client)
            repeat(2) {
                try{library.setSaved("person",listOf("spotify:playlist:abc"),true,"token");fail("429 hidden")}
                catch(e:Spotify.SpotifyException){assertEquals(429,e.statusCode)}
            }
            assertEquals(1,requests)
        }finally{client.close()}
    }
    @Test fun successfulHttpWithoutMembershipChangeIsNotReportedAsSaved() = runBlocking {
        var writes=0
        val client=HttpClient(MockEngine {req ->
            if(req.method==HttpMethod.Get)respond(snapshot(emptyList()),HttpStatusCode.OK)
            else{writes++;respond("{}",HttpStatusCode.OK)}
        })
        try {
            try{PlaylistLibrary(client).setSaved("person",listOf("spotify:playlist:abc"),true,"token");fail("False success")}
            catch(e:Spotify.SpotifyException){assertEquals(502,e.statusCode)}
            assertEquals(1,writes)
        }finally{client.close()}
    }
    @Test fun pageRevisionChangesCannotProduceAWrite() = runBlocking {
        var requests=0
        val client=HttpClient(MockEngine {req ->
            assertEquals(HttpMethod.Get,req.method);requests++
            if(requests==1)respond(snapshot(listOf("spotify:playlist:first"),"one",0,true,2),HttpStatusCode.OK)
            else respond(snapshot(listOf("spotify:playlist:second"),"two",1,false,2),HttpStatusCode.OK)
        })
        try {
            try{PlaylistLibrary(client).setSaved("person",listOf("spotify:playlist:abc"),true,"token");fail("Stale revision accepted")}
            catch(e:Spotify.SpotifyException){assertEquals(409,e.statusCode)}
            assertEquals(2,requests)
        }finally{client.close()}
    }
    @Test fun tracksCannotBeRemovedThroughLibraryPlaylistRoute() = runBlocking {
        var requests=0
        val client=HttpClient(MockEngine {requests++;respond("{}")})
        try{
            try{PlaylistLibrary(client).setSaved("person",listOf("spotify:track:abc"),false,"token");fail("Wrong URI type accepted")}
            catch(_:IllegalArgumentException){}
            assertEquals(0,requests)
        }finally{client.close()}
    }
}
