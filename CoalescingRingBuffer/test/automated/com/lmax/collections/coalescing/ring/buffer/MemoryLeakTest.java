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
