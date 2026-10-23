package com.music.spotui.ui.notification

/**
 * Prevent duplicate Radio continuation while the same end-of-queue lookup runs.
 * New playback generations or song selections supersede older requests.
 * Called only on the playback service main thread.
 */
class RadioContinuationGate {
    data class Request(val generation: Long, val songId: Int, val tasteRevision: Long)
    private var active: Request? = null

    fun begin(request: Request): Boolean {
        if (active == request) return false
        active = request
        return true
    }

    fun finish(request: Request) {
        if (active == request) active = null
    }
}
