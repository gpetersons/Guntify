package com.music.spotui.ui.notification

/** Avoid notifying Android Auto when listening-history storage changes but browse rows do not. */
object AndroidAutoBrowseRefreshPolicy {
    fun changed(previous: List<CarBrowsePolicy.Entry>?, current: List<CarBrowsePolicy.Entry>): Boolean =
        previous != null && previous != current
}
