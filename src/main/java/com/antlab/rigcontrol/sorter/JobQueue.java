package com.antlab.rigcontrol.sorter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class JobQueue {
    private final Deque<String> queue = new ArrayDeque<>();

    public synchronized void enqueue(String fileId) {
        if (fileId == null) {
            return;
        }
        queue.addLast(fileId);
    }

    public synchronized void clear() {
        queue.clear();
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized List<String> pollBatch(int size) {
        List<String> batch = new ArrayList<>();
        int count = Math.max(1, size);
        while (!queue.isEmpty() && batch.size() < count) {
            batch.add(queue.removeFirst());
        }
        return batch;
    }
}
