package com.cwnu.kitchenmood.timer;

import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;

public class KitchenTimerTest {
    @Test public void elapsedTimeSurvivesDelayedScreenRefresh() {
        AtomicLong clock = new AtomicLong(1_000L);
        KitchenTimer timer = new KitchenTimer(clock::get);
        timer.setMinutes(3);
        timer.start();
        clock.addAndGet(75_000L);
        assertEquals(105_000L, timer.getRemainingMillis());
        assertFalse(timer.tick());
        clock.addAndGet(200_000L);
        assertTrue(timer.tick());
        assertEquals(0L, timer.getRemainingMillis());
        assertTrue(timer.isComplete());
        assertFalse(timer.isRunning());
        assertFalse(timer.tick());
    }

    @Test public void pauseTimeDoesNotReduceRemainingTime() {
        AtomicLong clock = new AtomicLong(5_000L);
        KitchenTimer timer = new KitchenTimer(clock::get);
        timer.setMinutes(1);
        timer.start();
        clock.addAndGet(12_000L);
        timer.pause();
        clock.addAndGet(500_000L);
        assertEquals(48_000L, timer.getRemainingMillis());
        timer.start();
        clock.addAndGet(8_000L);
        assertEquals(40_000L, timer.getRemainingMillis());
    }

    @Test public void rotationRestoresOriginalDeadline() {
        AtomicLong clock = new AtomicLong(1_000L);
        KitchenTimer original = new KitchenTimer(clock::get);
        original.setMinutes(5);
        original.start();
        clock.addAndGet(20_000L);
        long remaining = original.getRemainingMillis();
        long deadline = original.getDeadline();
        clock.addAndGet(10_000L);
        KitchenTimer restored = new KitchenTimer(clock::get);
        restored.restore(original.getMinutes(), remaining, true, deadline);
        assertEquals(270_000L, restored.getRemainingMillis());
        assertEquals(deadline, restored.getDeadline());
        assertTrue(restored.isRunning());
    }

    @Test public void changingMinutesCancelsRunningTimer() {
        AtomicLong clock = new AtomicLong(1_000L);
        KitchenTimer timer = new KitchenTimer(clock::get);
        timer.start();
        timer.setMinutes(0);
        assertEquals(1, timer.getMinutes());
        assertEquals(60_000L, timer.getRemainingMillis());
        assertFalse(timer.isRunning());
        timer.setMinutes(Integer.MAX_VALUE);
        assertEquals(60, timer.getMinutes());
        assertEquals(3_600_000L, timer.getRemainingMillis());
    }

    @Test public void finishingDuringPauseStillReportsCompletionOnce() {
        AtomicLong clock = new AtomicLong(1_000L);
        KitchenTimer timer = new KitchenTimer(clock::get);
        timer.setMinutes(1);
        timer.start();
        clock.addAndGet(60_001L);
        timer.pause();
        assertTrue(timer.tick());
        assertFalse(timer.tick());
        timer.reset();
        assertEquals(60_000L, timer.getRemainingMillis());
        assertFalse(timer.isComplete());
    }

    @Test public void restoringExpiredRunningSnapshotReportsCompletionOnce() {
        AtomicLong clock = new AtomicLong(1_000L);
        KitchenTimer original = new KitchenTimer(clock::get);
        original.setMinutes(1);
        original.start();
        clock.addAndGet(60_000L);
        assertEquals(0L, original.getRemainingMillis());
        assertTrue(original.isRunning());

        KitchenTimer restored = new KitchenTimer(clock::get);
        restored.restore(original.getMinutes(), original.getRemainingMillis(),
                original.isRunning(), original.getDeadline());
        assertTrue(restored.isComplete());
        assertFalse(restored.isRunning());
        assertTrue(restored.tick());
        assertFalse(restored.tick());
    }

    @Test public void restoringReportedCompletionDoesNotReportAgain() {
        AtomicLong clock = new AtomicLong(1_000L);
        KitchenTimer original = new KitchenTimer(clock::get);
        original.setMinutes(1);
        original.start();
        clock.addAndGet(60_000L);
        assertTrue(original.tick());

        KitchenTimer restored = new KitchenTimer(clock::get);
        restored.restore(original.getMinutes(), original.getRemainingMillis(),
                original.isRunning(), original.getDeadline());
        assertEquals(0L, restored.getRemainingMillis());
        assertTrue(restored.isComplete());
        assertFalse(restored.isRunning());
        assertFalse(restored.tick());
    }
}
