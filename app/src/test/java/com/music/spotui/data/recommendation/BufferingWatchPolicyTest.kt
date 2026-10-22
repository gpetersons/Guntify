package com.music.spotui.data.recommendation
import com.music.spotui.audio.BufferingWatchPolicy
import com.music.spotui.audio.BufferingWatchPolicy.Decision.*
import org.junit.Assert.*
import org.junit.Test
class BufferingWatchPolicyTest {
 @Test fun initialBufferingBeforePlayArmsWhenPlayArrives(){val gate=BufferingWatchPolicy();assertEquals(STOP,gate.observe(1,true,false));assertEquals(START,gate.observe(1,true,true))}
 @Test fun playBeforeBufferingArmsOnBuffering(){val gate=BufferingWatchPolicy();assertEquals(STOP,gate.observe(1,false,true));assertEquals(START,gate.observe(1,true,true))}
 @Test fun duplicateEventsDoNotRestartDeadline(){val gate=BufferingWatchPolicy();assertEquals(START,gate.observe(1,true,true));repeat(10){assertEquals(KEEP,gate.observe(1,true,true))}}
 @Test fun pauseCancelsAndResumeArmsAgain(){val gate=BufferingWatchPolicy();gate.observe(1,true,true);assertEquals(STOP,gate.observe(1,true,false));assertEquals(START,gate.observe(1,true,true))}
 @Test fun readyCancelsAndLaterBufferingIsANewWatch(){val gate=BufferingWatchPolicy();gate.observe(1,true,true);assertEquals(STOP,gate.observe(1,false,true));assertEquals(START,gate.observe(1,true,true))}
 @Test fun anotherVisitNeedsItsOwnDeadlineEvenForSameSong(){val gate=BufferingWatchPolicy();gate.observe(1,true,true);assertEquals(START,gate.observe(3,true,true))}
 @Test fun swappingPlayerArmsANewWatchEvenForSameVisit(){val gate=BufferingWatchPolicy();assertEquals(START,gate.observe(1,true,true,10));assertEquals(START,gate.observe(1,true,true,20))}

}
