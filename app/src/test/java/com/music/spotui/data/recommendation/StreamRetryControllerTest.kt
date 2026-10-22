package com.music.spotui.data.recommendation

import com.music.spotui.audio.StreamRetryController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StreamRetryControllerTest {
    private class Harness(scope: CoroutineScope) {
        var ticket = StreamRetryController.Ticket("A", 1)
        var audible = false
        var attempts = 0
        var skips = 0
        var hasNext = true
        val controller: StreamRetryController = StreamRetryController(scope, { ticket }, { audible },
            retry = { attempts++; ticket = it.copy(visit = it.visit + 1); changed() },
            skip = {
                if (!hasNext) false else {
                    skips++; ticket = StreamRetryController.Ticket("B", ticket.visit + 1); changed(); true
                }
            })
        fun changed() = controller.requestChanged()
        fun fail() = controller.failed(ticket)
    }
    @Test fun retryWaitsTenSecondsAndDoesNotOverlap() = runTest {
        val h = Harness(backgroundScope)
        h.fail(); h.fail(); runCurrent()
        advanceTimeBy(9_999); runCurrent(); assertEquals(0, h.attempts)
        advanceTimeBy(1); runCurrent(); assertEquals(1, h.attempts)
        advanceTimeBy(30_000); runCurrent(); assertEquals(1, h.attempts)
    }
    @Test fun retriesTwiceSkipsOnceThenHoldsSecondSong() = runTest {
        val h = Harness(backgroundScope)
        repeat(2) { h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent() }
        assertEquals(2, h.attempts); assertEquals(0, h.skips)
        h.fail(); assertEquals(1, h.skips); assertEquals("B", h.ticket.query)
        repeat(5) { h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent() }
        assertEquals(7, h.attempts); assertEquals(1, h.skips)
    }
    @Test fun pauseOrStopCancelsPendingRetryAndBudget() = runTest {
        val h = Harness(backgroundScope)
        h.fail(); runCurrent(); advanceTimeBy(5_000); h.changed()
        advanceTimeBy(20_000); runCurrent()
        assertEquals(0, h.attempts); assertFalse(h.controller.isRecovering)
    }
    @Test fun manualSelectionCannotBeOverriddenByOldTimer() = runTest {
        val h = Harness(backgroundScope)
        h.fail(); runCurrent(); h.ticket = StreamRetryController.Ticket("C", 2); h.changed()
        advanceTimeBy(10_000); runCurrent(); assertEquals(0, h.attempts)
    }
    @Test fun changedVisitIsAlsoGuardedWithoutCallback() = runTest {
        val h = Harness(backgroundScope)
        h.fail(); runCurrent(); h.ticket = h.ticket.copy(visit = 2)
        advanceTimeBy(10_000); runCurrent(); assertEquals(0, h.attempts)
        assertFalse(h.controller.isRecovering)
    }
    @Test fun audibleRecoveryCancelsTimerAndResetsBudget() = runTest {
        val h = Harness(backgroundScope)
        repeat(2) { h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent() }
        h.fail(); h.fail(); runCurrent(); h.controller.recovered()
        advanceTimeBy(10_000); runCurrent(); assertEquals(2, h.attempts)
        h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent()
        assertEquals(3, h.attempts); assertEquals(1, h.skips)
    }
    @Test fun noUpcomingTrackKeepsRetryingInsteadOfWrapping() = runTest {
        val h = Harness(backgroundScope); h.hasNext = false
        repeat(6) { h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent() }
        assertEquals(6, h.attempts); assertEquals(0, h.skips); assertEquals("A", h.ticket.query)
    }
    @Test fun staleAndDuplicateFailuresDoNotConsumeAnotherAttempt() = runTest {
        val h = Harness(backgroundScope)
        h.controller.failed(StreamRetryController.Ticket("old", 0))
        h.fail(); h.fail(); h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent()
        h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent()
        assertEquals(2, h.attempts); assertEquals(0, h.skips)
    }
    @Test fun audioReturningDuringWaitPreventsAnotherLookup() = runTest {
        val h = Harness(backgroundScope)
        h.fail(); runCurrent(); h.audible = true
        advanceTimeBy(10_000); runCurrent(); assertEquals(0, h.attempts)
        assertFalse(h.controller.isRecovering)
    }
    @Test fun newUserRequestResetsTheHeldSongBudget() = runTest {
        val h = Harness(backgroundScope)
        repeat(2) { h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent() }
        h.fail(); h.changed()
        repeat(2) { h.fail(); runCurrent(); advanceTimeBy(10_000); runCurrent() }
        assertEquals(1, h.skips); assertEquals(4, h.attempts)
    }
    @Test fun waitingFeedbackDoesNotDuplicateForSameFailure() = runTest {
        var notices=0
        val ticket=StreamRetryController.Ticket("A",1)
        val controller=StreamRetryController(backgroundScope,{ticket},{false},{},{false},{notices++})
        controller.failed(ticket);controller.failed(ticket)
        assertEquals(1,notices)
    }

}
