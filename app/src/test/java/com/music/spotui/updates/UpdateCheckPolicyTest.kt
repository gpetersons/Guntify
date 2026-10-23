package com.music.spotui.updates
import org.junit.Assert.*
import org.junit.Test
class UpdateCheckPolicyTest {
    @Test fun coldLaunchAlwaysRevalidatesEvenAfterRecentCheck() { assertTrue(UpdateCheckPolicy.shouldCheck(true, 5000, 4999)) }
    @Test fun warmResumeUsesHourlyThrottle() { assertFalse(UpdateCheckPolicy.shouldCheck(false, 5000, 4999)); assertTrue(UpdateCheckPolicy.shouldCheck(false, 3605000, 5000)) }
    @Test fun missingTimestampAndClockRollbackDoNotDisableUpdates() { assertTrue(UpdateCheckPolicy.shouldCheck(false, 5000, 0)); assertTrue(UpdateCheckPolicy.shouldCheck(false, 5000, 6000)) }
}
