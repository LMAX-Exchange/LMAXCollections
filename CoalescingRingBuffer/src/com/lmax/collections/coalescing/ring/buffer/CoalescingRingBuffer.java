package com.lmax.collections.coalescing.ring.buffer;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static java.lang.Math.min;

@SuppressWarnings("unchecked")
public final class CoalescingRingBuffer<K, V> implements CoalescingBuffer<K, V> {

    private volatile long nextWrite = 1; // the next write index
    private volatile long lastCleaned = 0; // the last index that was nulled out by the producer
    private volatile long rejectionCount = 0;
    private final K[] keys;
    private final AtomicReferenceArray<V> values;

    private final K nonCollapsibleKey = (K) new Object();
    private final int mask;
    private final int capacity;

    private volatile long nextRead = 1; // the oldest slot that is is safe to write to
    private volatile long lastRead = 0; // the newest slot that it is safe to overwrite

    public CoalescingRingBuffer(int capacity) {
        checkIsPowerOfTwo(capacity);
        this.mask = capacity - 1;
        this.capacity = capacity;

        this.keys = (K[]) new Object[capacity];
        this.values = new AtomicReferenceArray<V>(capacity);
    }

    private void checkIsPowerOfTwo(int capacity) {
        if (Integer.bitCount(capacity) != 1) {
            throw new IllegalArgumentException("capacity (" + capacity + ") must be a power of two");
        }
    }

    @Override
    public int size() {
        return (int) (nextWrite - lastRead - 1);
    }

    @Override
    public int capacity() {
        return capacity;
    }

    public long rejectionCount() {
        return rejectionCount;
    }

    public long nextWrite() {
        return nextWrite;
    }

    public long nextRead() {
        return nextRead;
    }

    @Override
    public boolean isEmpty() {
        return nextRead == nextWrite;
    }

    @Override
    public boolean isFull() {
        return size() == capacity;
    }

    @Override
    public boolean offer(K key, V value) {
        long nextWrite = this.nextWrite;

        for (long readPosition = nextRead; readPosition < nextWrite; readPosition++) {
            int index = mask(readPosition);

            if(key.equals(keys[index])) {
                values.set(index, value);

                if (nextRead <= readPosition) {  // check that the reader has not read it yet
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
            rejectionCount++;
            return false;
        }

        cleanUp();
        store(key, value);
        return true;
    }

    private void cleanUp() {
        long lastRead = this.lastRead;
        long lastCleaned = this.lastCleaned;

        if (lastRead == lastCleaned) {
            return;
        }

        for (long nextClean = lastCleaned + 1; nextClean <= lastRead; nextClean++) {
            int index = mask(nextClean);
            keys[index] = null;
            values.set(index, null);
        }

        this.lastCleaned = lastRead;
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
        claimUpTo(nextWrite);
        return fill(bucket);
    }

    @Override
    public int poll(Collection<? super V> bucket, int maxItems) {
        claimUpTo(min(nextRead + maxItems, nextWrite));
        return fill(bucket);
    }

    private void claimUpTo(long claimIndex) {
        nextRead = claimIndex;
    }

    private int fill(Collection<? super V> bucket) {
        long nextRead = this.nextRead;
        long lastRead = this.lastRead;

        for (long readIndex = lastRead + 1; readIndex < nextRead; readIndex++) {
            int index = mask(readIndex);
            bucket.add(values.get(index));
        }

        int readCount = (int) (nextRead - lastRead - 1);
        this.lastRead = nextRead - 1;
        return readCount;
    }

    private int mask(long value) {
        return ((int) value) & mask;
    }

}