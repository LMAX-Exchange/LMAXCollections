package com.lmax.collections.coalescing.ring.buffer;

import java.util.Collection;

public final class EmptyCoalescingBuffer<K, V> implements CoalescingBuffer<K, V> {
    public static final EmptyCoalescingBuffer INSTANCE = new EmptyCoalescingBuffer();

    @SuppressWarnings("unchecked")
    public static <K,V> CoalescingBuffer<K,V> emptyBuffer() {
        return INSTANCE;
    }

    private EmptyCoalescingBuffer() {}

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int capacity() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean isFull() {
        return false;
    }

    @Override
    public boolean offer(K key, V value) {
        return false;
    }

    @Override
    public boolean offer(V value) {
        return false;
    }

    @Override
    public int poll(Collection<? super V> bucket) {
        return 0;
    }

    @Override
    public int poll(Collection<? super V> bucket, int maxItems) {
        return 0;
    }

}