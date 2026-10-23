package com.music.spotui.updates

/** Cold starts revalidate the release; warm resumes avoid repeated requests. */
internal object UpdateCheckPolicy {
    fun shouldCheck(coldStart: Boolean, now: Long, lastAttempt: Long): Boolean =
        coldStart || lastAttempt <= 0L || now < lastAttempt || now - lastAttempt >= 60 * 60_000L
}
