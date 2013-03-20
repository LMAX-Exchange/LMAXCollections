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

package com.lmax.collections.coalescing.ring.buffer;

import org.junit.Test;

import java.util.ArrayList;

import static com.lmax.collections.coalescing.ring.buffer.MarketSnapshot.createMarketSnapshot;
import static org.junit.Assert.assertEquals;

public class ThreadSafetyTest {

    private static final int NUMBER_OF_INSTRUMENTS = 5000000;
    private static final long POISON_PILL = -1;

    private static final long FIRST_BID = 3;
    private static final long SECOND_BID = 4;

    private static final long FIRST_ASK = 5;
    private static final long SECOND_ASK = 6;


    private static class Producer extends Thread {

        private final CoalescingBuffer<Long, MarketSnapshot> snapshotBuffer;

        private Producer(CoalescingBuffer<Long, MarketSnapshot> snapshotBuffer) {
            super("producer");
            this.snapshotBuffer = snapshotBuffer;
        }

        @Override
        public void run() {
            for (long key = 0; key < NUMBER_OF_INSTRUMENTS; key++) {
                put(key, FIRST_BID, FIRST_ASK);
                put(key, SECOND_BID, SECOND_ASK);
            }

            put(POISON_PILL, POISON_PILL, POISON_PILL);
        }

        private void put(long key, long bid, long ask) {
            boolean success = snapshotBuffer.offer(key, createMarketSnapshot(key, bid, ask));
            if (!success) {
                throw new AssertionError("adding of key " + key + " failed");
            }
        }
    }

    private static class Consumer extends Thread {
        private final MarketSnapshot[] snapshots = new MarketSnapshot[NUMBER_OF_INSTRUMENTS];
        private final CoalescingBuffer<Long, MarketSnapshot> snapshotBuffer;
        private boolean useLimitedRead;

        private Consumer(CoalescingBuffer<Long, MarketSnapshot> snapshotBuffer) {
            super("consumer");
            this.snapshotBuffer = snapshotBuffer;
        }

        @Override
        public void run() {
            ArrayList<MarketSnapshot> bucket = new ArrayList<MarketSnapshot>();

            while (true) {
                fill(bucket);

                for (MarketSnapshot snapshot : bucket) {
                    if (snapshot.getInstrumentId() == POISON_PILL) {
                        return;
                    }

                    snapshots[indexOf(snapshot)] = snapshot;
                }

                bucket.clear();
            }
        }

        private int indexOf(MarketSnapshot snapshot) {
            return (int) snapshot.getInstrumentId();
        }

        private void fill(ArrayList<MarketSnapshot> bucket) {
            if (useLimitedRead) {
                snapshotBuffer.poll(bucket, 1);
            }
            else {
                snapshotBuffer.poll(bucket);
            }
            useLimitedRead = !useLimitedRead;
        }
    }

    @Test
    public void shouldSeeLastPrices() throws InterruptedException {
        CoalescingBuffer<Long, MarketSnapshot> buffer = new CoalescingRingBuffer<Long, MarketSnapshot>(1 << 20);

        Producer producer = new Producer(buffer);
        Consumer consumer = new Consumer(buffer);

        producer.start();
        consumer.start();

        consumer.join();

        for (int instrument = 0; instrument < NUMBER_OF_INSTRUMENTS; instrument++) {
            MarketSnapshot snapshot = consumer.snapshots[instrument];

            assertEquals("bid for instrument " + instrument + ":", SECOND_BID, snapshot.getBid());
            assertEquals("ask for instrument " + instrument + ":", SECOND_ASK, snapshot.getAsk());
        }
    }

}
