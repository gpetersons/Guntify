package com.music.spotui.data.api

import android.content.Context
import com.metrolist.spotify.Spotify

/** Retry once only after a definite expired-token rejection. Never fake a successful remote change. */
object SpotifyMutations {
    suspend fun <T> run(context: Context, action: suspend () -> Result<T>): Result<T> {
        if (!SpotifyTokenProvider.ensureToken(context)) return Result.failure(Spotify.SpotifyException(401, "Sign in to Spotify again"))
        val result = attempt(action)
        if ((result.exceptionOrNull() as? Spotify.SpotifyException)?.statusCode == 401 && SpotifyTokenProvider.ensureToken(context, force = true)) return attempt(action)
        return result
    }
    private suspend fun <T> attempt(action: suspend () -> Result<T>): Result<T> =
        kotlinx.coroutines.withTimeoutOrNull(20_000L) { action() }
            ?: Result.failure(IllegalStateException("Spotify response timed out; check your library before retrying"))

    fun describe(error: Throwable?): String {
        if (error?.message?.contains("timed out", ignoreCase = true) == true) return "Spotify did not respond in time. Check your library before retrying: the change may already have been applied."
        val failure = error as? Spotify.SpotifyException
        val response = when (failure?.statusCode) {
            401 -> "Spotify sign-in expired. Sign in again, then retry."
            403 -> "Spotify rejected this account's write authorization (HTTP 403)."
            429 -> "Spotify returned HTTP 429 for this request. This does not establish whether your account or the app's API access is restricted." +
                if (failure.retryAfterSec > 0) " Retry after ${failure.retryAfterSec} seconds." else ""
            400, 404, 405, 412 -> "Spotify rejected this request (HTTP ${failure.statusCode})."
            502 -> "Spotify's response could not be confirmed. Check your library before retrying."
            else -> "Could not complete the Spotify change. Check your connection and retry."
        }
        return response + (if (failure?.endpoint?.isNotBlank() == true) "\nRequest: ${failure.endpoint}" else "") +
            (if (failure?.detail?.isNotBlank() == true) "\nSpotify: ${failure.detail}" else "")
    }
}
