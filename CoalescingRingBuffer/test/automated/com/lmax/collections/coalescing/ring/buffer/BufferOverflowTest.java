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
import java.util.List;

import static org.junit.Assert.assertFalse;

public class BufferOverflowTest {
    private static final int POISON_PILL = -1;

    private static class Producer extends Thread {

        private final CoalescingBuffer<Integer, Integer> buffer;
        private boolean hasOverflowed;

        Producer(CoalescingBuffer<Integer, Integer> buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            for (int run = 0; run < 1000000; run++) {
                for (int message = 0; message < 10; message++) {
                    boolean success = buffer.offer(message, message);

                    if (!success) {
                        hasOverflowed = true;
                        buffer.offer(POISON_PILL);
                        return;
                    }
                }
            }

            buffer.offer(POISON_PILL);
        }

    }

    private static class Consumer extends Thread {

        public static final int CAPACITY = 100;
        private final CoalescingBuffer<Integer, Integer> buffer;

        Consumer(CoalescingBuffer<Integer, Integer> buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            List<Integer> values = new ArrayList<Integer>(CAPACITY);

            while (true) {
                buffer.poll(values, CAPACITY);

                if (values.contains(POISON_PILL)) {
                    return;
                }
            }
        }

    }

    @Test
    public void shouldBeAbleToReuseCapacity() throws Exception {
        CoalescingBuffer<Integer, Integer> buffer = new CoalescingRingBuffer<Integer, Integer>(32);

        Producer producer = new Producer(buffer);
        Consumer consumer = new Consumer(buffer);

        producer.start();
        consumer.start();

        producer.join();
        assertFalse("ring buffer has overflowed", producer.hasOverflowed);
    }

}
