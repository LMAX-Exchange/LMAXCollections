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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemoryLeakTest {

    private static class CountingKey {
        private final int id;
        private final AtomicInteger counter;

        private CountingKey(int id, AtomicInteger counter) {
            this.id = id;
            this.counter = counter;
        }


        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;

            CountingKey other = (CountingKey) object;
            return this.id == other.id;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        protected void finalize() throws Throwable {
            counter.incrementAndGet();
        }
    }

    private static class CountingValue {
        private final AtomicInteger counter;

        private CountingValue(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        protected void finalize() throws Throwable {
            counter.incrementAndGet();
        }
    }

    @Test
    public void shouldNotHaveMemoryLeaks() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        CoalescingBuffer<CountingKey, CountingValue> buffer = new CoalescingRingBuffer<CountingKey, CountingValue>(16);
        buffer.offer(new CountingValue(counter));

        buffer.offer(new CountingKey(1, counter), new CountingValue(counter));
        buffer.offer(new CountingKey(2, counter), new CountingValue(counter));
        buffer.offer(new CountingKey(1, counter), new CountingValue(counter));

        buffer.offer(new CountingValue(counter));

        assertEquals(4, buffer.size());
        buffer.poll(new ArrayList<CountingValue>(), 1);
        buffer.poll(new ArrayList<CountingValue>());
        assertTrue(buffer.isEmpty());

        buffer.offer(null); // to trigger the clean
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(100);
        }

        assertEquals(8, counter.get());
    }
}
