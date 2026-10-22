package com.metrolist.music.utils.potoken

class PoTokenResult(
    val playerRequestPoToken: String,
    val streamingDataPoToken: String,
) {
    companion object {
        /** The player proof is minted per video; the CDN proof belongs to the session. */
        suspend fun forVideo(sessionToken: String, videoId: String, mint: suspend (String) -> String): PoTokenResult =
            PoTokenResult(playerRequestPoToken = mint(videoId), streamingDataPoToken = sessionToken)
    }
}
