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