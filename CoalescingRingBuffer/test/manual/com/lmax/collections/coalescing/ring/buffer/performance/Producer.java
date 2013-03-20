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

import static com.lmax.collections.coalescing.ring.buffer.MarketSnapshot.createMarketSnapshot;

final class Producer extends Thread {

    private final CoalescingBuffer<Long, MarketSnapshot> buffer;
    private final long numberOfUpdates;
    private final MarketSnapshot poisonPill;
    private final StopWatch stopWatch;
    private final int numberOfInstruments;
    private final MarketSnapshot[] snapshots;
    private int nextSnapshot;

    Producer(CoalescingBuffer<Long, MarketSnapshot> buffer, int numberOfInstruments, long numberOfUpdates, MarketSnapshot poisonPill, StopWatch stopWatch) {
        super("producer");
        this.buffer = buffer;
        this.numberOfInstruments = numberOfInstruments;
        this.numberOfUpdates = numberOfUpdates;
        this.poisonPill = poisonPill;
        this.stopWatch = stopWatch;
        this.snapshots = createSnapshots(numberOfInstruments);
    }

    private MarketSnapshot[] createSnapshots(int numberOfInstruments) {
        MarketSnapshot[] snapshots = new MarketSnapshot[numberOfInstruments];

        for (int i = 0; i < numberOfInstruments; i++) {
            int bid = numberOfInstruments * i;
            int ask = numberOfInstruments * numberOfInstruments * i;

            snapshots[i] = createMarketSnapshot(i, bid, ask);
        }

        return snapshots;
    }

    @Override
    public void run() {
        stopWatch.producerIsReady();

        for (long i = 1; i <= numberOfUpdates; i++) {
            put(nextId(i), nextSnapshot());
        }

        put(poisonPill.getInstrumentId(), poisonPill);
    }

    /**
     * simulates some instruments update much more frequently than others
     */
    private long nextId(long counter) {
        int register = (int) counter;

        for (int i = 1; i < numberOfInstruments; i++) {
            if ((register & 1) == 1) {
                return i;
            }

            register >>= 1;
        }

        return numberOfInstruments;
    }

    private MarketSnapshot nextSnapshot() {
        if (nextSnapshot == numberOfInstruments) {
            nextSnapshot = 0;
        }

        return snapshots[nextSnapshot++];
    }

    private void put(long id, MarketSnapshot snapshot) {
        boolean success = buffer.offer(id, snapshot);

        if (!success) {
            throw new AssertionError("failed to add instrument id " + snapshot.getInstrumentId());
        }
    }

}