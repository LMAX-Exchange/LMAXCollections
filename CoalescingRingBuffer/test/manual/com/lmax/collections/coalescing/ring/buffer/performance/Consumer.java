/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
