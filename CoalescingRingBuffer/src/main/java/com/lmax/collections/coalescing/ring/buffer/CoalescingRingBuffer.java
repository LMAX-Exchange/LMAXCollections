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

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static java.lang.Math.min;

public final class CoalescingRingBuffer<K, V> implements CoalescingBuffer<K, V> {

    private volatile long nextWrite = 1; // the next write index
    private long lastCleaned = 0; // the last index that was nulled out by the producer
    private final AtomicLong rejectionCount = new AtomicLong(0);
    private final K[] keys;
    private final AtomicReferenceArray<V> values;

    @SuppressWarnings("unchecked")
    private final K nonCollapsibleKey = (K) new Object();
    private final int mask;
    private final int capacity;

    private volatile long firstWrite = 1; // the oldest slot that is is safe to write to
    private final AtomicLong lastRead = new AtomicLong(0); // the newest slot that it is safe to overwrite

    @SuppressWarnings("unchecked")
    public CoalescingRingBuffer(int capacity) {
        this.capacity = nextPowerOfTwo(capacity);
        this.mask = this.capacity - 1;

        this.keys = (K[]) new Object[this.capacity];
        this.values = new AtomicReferenceArray<V>(this.capacity);
    }

    private int nextPowerOfTwo(int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    @Override
    public int size() {

        // loop until you get a consistent read of both volatile indices
        while (true) {
            long lastReadBefore     = lastRead.get();
            long currentNextWrite   = this.nextWrite;
            long lastReadAfter      = lastRead.get();

            if (lastReadBefore == lastReadAfter) {
                return (int) (currentNextWrite - lastReadBefore) - 1;
            }
        }
    }

    @Override
    public int capacity() {
        return capacity;
    }

    public long rejectionCount() {
        return rejectionCount.get();
    }

    public long nextWrite() {
        return nextWrite;
    }

    public long firstWrite() {
        return firstWrite;
    }

    @Override
    public boolean isEmpty() {
        return firstWrite == nextWrite;
    }

    @Override
    public boolean isFull() {
        return size() == capacity;
    }

    @Override
    public boolean offer(K key, V value) {
        long nextWrite = this.nextWrite;

        for (long updatePosition = firstWrite; updatePosition < nextWrite; updatePosition++) {
            int index = mask(updatePosition);

            if(Objects.equals(key, keys[index])) {
                values.set(index, value);

                if (updatePosition >= firstWrite) {  // check that the reader has not read beyond our update point yet
                    return true;
                } else {
                    break;
                }
            }
        }

        return add(key, value);
    }

    @Override
    public boolean offer(V value) {
        return add(nonCollapsibleKey, value);
    }

    private boolean add(K key, V value) {
        if (isFull()) {
            rejectionCount.lazySet(rejectionCount.get() + 1);
            return false;
        }

        cleanUp();
        store(key, value);
        return true;
    }

    private void cleanUp() {
        long lastRead = this.lastRead.get();

        if (lastRead == lastCleaned) {
            return;
        }

        while (lastCleaned < lastRead) {
            int index = mask(++lastCleaned);
            keys[index] = null;
            values.lazySet(index, null);
        }
    }

    private void store(K key, V value) {
        long nextWrite = this.nextWrite;
        int index = mask(nextWrite);

        keys[index] = key;
        values.set(index, value);

        this.nextWrite = nextWrite + 1;
    }

    @Override
    public int poll(Collection<? super V> bucket) {
        return fill(bucket, nextWrite);
    }

    @Override
    public int poll(Collection<? super V> bucket, int maxItems) {
        long claimUpTo = min(firstWrite + maxItems, nextWrite);
        return fill(bucket, claimUpTo);
    }

    private int fill(Collection<? super V> bucket, long claimUpTo) {
        firstWrite = claimUpTo;
        long lastRead = this.lastRead.get();

        for (long readIndex = lastRead + 1; readIndex < claimUpTo; readIndex++) {
            int index = mask(readIndex);
            bucket.add(values.get(index));
        }

        this.lastRead.lazySet(claimUpTo - 1);
        return (int) (claimUpTo - lastRead - 1);
    }

    private int mask(long value) {
        return ((int) value) & mask;
    }

}