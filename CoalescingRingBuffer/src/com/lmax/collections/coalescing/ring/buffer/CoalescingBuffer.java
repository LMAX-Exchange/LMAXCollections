package com.lmax.collections.coalescing.ring.buffer;

import java.util.Collection;

public interface CoalescingBuffer<K, V> {

    /**
     * @return the current size of the buffer
     */
    int size();

    /**
     * @return the maximum size of the buffer
     */
    int capacity();

    boolean isEmpty();

    boolean isFull();

    /**
     * Add a value to be collapsed on the give key
     *
     * @param key the key on which to collapse the value
     *        equality is determined by the equals method
     * @return true if the value was added or false if the buffer was full
     */
    boolean offer(K key, V value);

    /**
     * Add a value that will never be collapsed
     *
     * @return true if the value was added or false if the buffer was full
     */
    boolean offer(V value);

    /**
     * add all available items to the given bucket
     *
     * @return the number of items added
     */
    int poll(Collection<? super V> bucket);

    /**
     * add a maximum number of items to the given bucket
     *
     * @return the number of items added
     */
    int poll(Collection<? super V> bucket, int maxItems);

}