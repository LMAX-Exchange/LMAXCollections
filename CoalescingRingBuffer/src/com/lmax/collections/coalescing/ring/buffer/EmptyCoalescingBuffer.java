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