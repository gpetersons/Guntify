package com.music.spotui.data.recommendation

import com.music.spotui.audio.AndroidAutoSkipPolicy
import com.music.spotui.ui.notification.AndroidAutoBrowseRefreshPolicy
import com.music.spotui.ui.notification.CarBrowsePolicy
import com.music.spotui.ui.notification.RadioContinuationGate
import org.junit.Assert.*
import org.junit.Test

class AndroidAutoBeta3Test {
    @Test fun radioContinuationStartsOnceForConcurrentEndSignals() {
        val gate = RadioContinuationGate()
        val key = RadioContinuationGate.Request(10, 42, 0)
        assertTrue(gate.begin(key))
        assertFalse(gate.begin(key))
        gate.finish(key)
        assertTrue(gate.begin(key))
    }

    @Test fun canceledOldContinuationCannotReleaseNewSong() {
        val gate = RadioContinuationGate()
        val old = RadioContinuationGate.Request(10, 42, 0)
        val newer = RadioContinuationGate.Request(10, 44, 0)
        assertTrue(gate.begin(old))
        assertTrue(gate.begin(newer))
        gate.finish(old)
        assertFalse(gate.begin(newer))
        gate.finish(newer)
        assertTrue(gate.begin(newer))
    }

    @Test fun browseNotificationsRequireActualRowChanges() {
        val recent = listOf(CarBrowsePolicy.Entry("r1", "Radio One"))
        assertFalse(AndroidAutoBrowseRefreshPolicy.changed(null, recent))
        assertFalse(AndroidAutoBrowseRefreshPolicy.changed(recent, recent.toList()))
        assertTrue(AndroidAutoBrowseRefreshPolicy.changed(recent, listOf(CarBrowsePolicy.Entry("r2", "Radio Two"))))
        assertTrue(AndroidAutoBrowseRefreshPolicy.changed(recent, emptyList()))
        assertTrue(AndroidAutoBrowseRefreshPolicy.changed(recent, listOf(CarBrowsePolicy.Entry("r1", "Updated title"))))
    }

    @Test fun rapidManualNextIsCoalescedButAutoAndPreviousAreNot() {
        assertEquals(180L, AndroidAutoSkipPolicy.lookupDelayMs(manual = true, forward = true))
        assertEquals(0L, AndroidAutoSkipPolicy.lookupDelayMs(manual = false, forward = true))
        assertEquals(0L, AndroidAutoSkipPolicy.lookupDelayMs(manual = true, forward = false))
    }
}
