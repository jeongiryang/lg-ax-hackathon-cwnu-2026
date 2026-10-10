package com.cwnu.kitchenmood.timer;

import java.util.Objects;
import java.util.function.LongSupplier;

public final class KitchenTimer {
    private final LongSupplier clock;
    private int minutes = 3;
    private long remainingMillis = 180_000L;
    private long deadline;
    private boolean running;
    private boolean complete;
    private boolean completionPending;

    public KitchenTimer(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized void setMinutes(int minutes) {
        this.minutes = Math.max(1, Math.min(60, minutes));
        reset();
    }

    public synchronized void start() {
        if (running) return;
        if (remainingMillis <= 0) remainingMillis = durationMillis();
        deadline = clock.getAsLong() + remainingMillis;
        running = true;
        complete = false;
        completionPending = false;
    }

    public synchronized void pause() {
        if (!running) return;
        remainingMillis = Math.max(0L, deadline - clock.getAsLong());
        running = false;
        deadline = 0L;
        if (remainingMillis == 0L) {
            complete = true;
            completionPending = true;
        }
    }

    public synchronized void reset() {
        remainingMillis = durationMillis();
        deadline = 0L;
        running = false;
        complete = false;
        completionPending = false;
    }

    public synchronized boolean tick() {
        if (completionPending) {
            completionPending = false;
            return true;
        }
        if (!running) return false;
        remainingMillis = Math.max(0L, deadline - clock.getAsLong());
        if (remainingMillis > 0L) return false;
        running = false;
        complete = true;
        deadline = 0L;
        return true;
    }

    public synchronized long getRemainingMillis() {
        return running ? Math.max(0L, deadline - clock.getAsLong()) : remainingMillis;
    }

    public synchronized int getMinutes() {
        return minutes;
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized boolean isComplete() {
        return complete;
    }

    public synchronized long getDeadline() {
        return deadline;
    }

    public synchronized void restore(int minutes, long remainingMs, boolean running, long deadline) {
        this.minutes = Math.max(1, Math.min(60, minutes));
        remainingMillis = Math.max(0L, Math.min(durationMillis(), remainingMs));
        this.running = running && remainingMillis > 0L && deadline > 0L;
        this.deadline = this.running ? Math.min(deadline, clock.getAsLong() + remainingMillis) : 0L;
        complete = remainingMillis == 0L;
        completionPending = running && complete && deadline > 0L;
    }

    private long durationMillis() {
        return minutes * 60_000L;
    }
}
