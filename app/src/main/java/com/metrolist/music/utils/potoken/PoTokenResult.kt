package com.metrolist.music.utils.potoken

class PoTokenResult(
    val playerRequestPoToken: String,
    val streamingDataPoToken: String,
) {
    /** Some 2026 Google Video experiments accept a video-bound GVS proof, rather than a session proof. */
    val videoBoundStreamingDataPoToken: String get() = playerRequestPoToken

    companion object {
        /** Keep both proofs: CDN enforcement can differ per client, account and video. */
        suspend fun forVideo(sessionToken: String, videoId: String, mint: suspend (String) -> String): PoTokenResult =
            PoTokenResult(playerRequestPoToken = mint(videoId), streamingDataPoToken = sessionToken)
    }
}
