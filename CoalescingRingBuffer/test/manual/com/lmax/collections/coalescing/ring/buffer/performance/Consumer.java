package com.lmax.collections.coalescing.ring.buffer.performance;

import com.lmax.collections.coalescing.ring.buffer.CoalescingBuffer;
import com.lmax.collections.coalescing.ring.buffer.MarketSnapshot;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.nanoTime;

final class Consumer extends Thread {
    private final CoalescingBuffer<Long, MarketSnapshot> buffer;
    private final int numberOfInstruments;
    private final MarketSnapshot poisonPill;
    private final StopWatch stopWatch;

    final MarketSnapshot[] latestSnapshots;
    long readCounter;

    Consumer(CoalescingBuffer<Long, MarketSnapshot> buffer, int numberOfInstruments, MarketSnapshot poisonPill, StopWatch stopWatch) {
        super("consumer");
        this.buffer = buffer;
        this.numberOfInstruments = numberOfInstruments;
        this.poisonPill = poisonPill;
        this.stopWatch = stopWatch;
        this.latestSnapshots = new MarketSnapshot[numberOfInstruments];
    }

    @Override
    public void run() {
        List<MarketSnapshot> bucket = new ArrayList<MarketSnapshot>(numberOfInstruments);
        stopWatch.consumerIsReady();

        while (true)  {
            buffer.poll(bucket);

            for (int i = 0, snapshotsSize = bucket.size(); i < snapshotsSize; i++) {
                readCounter++;

                MarketSnapshot snapshot = bucket.get(i);
                if (snapshot == poisonPill) {
                    stopWatch.consumerIsDone();
                    return;
                }

                latestSnapshots[((int) snapshot.getInstrumentId())] = snapshot;
            }

            simulateProcessing();
            bucket.clear();
        }
    }

    private void simulateProcessing() {
        long sleepUntil = nanoTime() + 10 * 1000;
        while (nanoTime() < sleepUntil) {
            // busy spin to simulate processing
        }
    }

}
