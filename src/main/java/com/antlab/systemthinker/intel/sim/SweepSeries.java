package com.antlab.systemthinker.intel.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SweepSeries {
    private final String label;
    private final List<SweepPoint> points;

    public SweepSeries(String label, List<SweepPoint> points) {
        this.label = label;
        this.points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    public String getLabel() {
        return label;
    }

    public List<SweepPoint> getPoints() {
        return points;
    }
}
