package com.antlab.systemthinker.intel.sim;

public final class ReportEvent {
    private final int sourceId;
    private final ReportSignal signal;
    private final int timeObserved;
    private final int timeArrived;

    public ReportEvent(int sourceId, ReportSignal signal, int timeObserved, int timeArrived) {
        this.sourceId = sourceId;
        this.signal = signal;
        this.timeObserved = timeObserved;
        this.timeArrived = timeArrived;
    }

    public int getSourceId() {
        return sourceId;
    }

    public ReportSignal getSignal() {
        return signal;
    }

    public int getTimeObserved() {
        return timeObserved;
    }

    public int getTimeArrived() {
        return timeArrived;
    }
}
