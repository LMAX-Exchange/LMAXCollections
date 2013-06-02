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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.lmax.collections.coalescing.ring.buffer.MarketSnapshot.createMarketSnapshot;
import static org.junit.Assert.*;
import static org.junit.Assert.assertSame;

public class FunctionalTest {
    private final static MarketSnapshot VOD_SNAPSHOT_1 = createMarketSnapshot(1, 3, 4);
    private final static MarketSnapshot VOD_SNAPSHOT_2 = createMarketSnapshot(1, 5, 6);
    private final static MarketSnapshot BP_SNAPSHOT = createMarketSnapshot(2, 7, 8);

    protected CoalescingBuffer<Long, MarketSnapshot> buffer;

    @Before
    public void beforeEveryTest() {
        buffer = createBuffer(10);
    }

    public CoalescingBuffer<Long, MarketSnapshot> createBuffer(int capacity) {
        return new CoalescingRingBuffer<Long, MarketSnapshot>(capacity);
    }

    @Test
    public void shouldCorrectlyIncreaseTheCapacityToTheNextHigherPowerOfTwo() {
        checkCapacity(1024, createBuffer(1023));
        checkCapacity(1024, createBuffer(1024));
        checkCapacity(2048, createBuffer(1025));
    }

    private void checkCapacity(int capacity, CoalescingBuffer<Long, MarketSnapshot> buffer) {
        assertEquals(capacity, buffer.capacity());

        for (int i = 0; i < capacity; i++) {
            boolean success = buffer.offer(createMarketSnapshot(i, i, i));
            assertTrue(success);
        }
    }

    @Test
    public void shouldCorrectlyReportSize() {
        Collection<MarketSnapshot> snapshots = new ArrayList<MarketSnapshot>();

        buffer = createBuffer(2);
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());

        buffer.offer(BP_SNAPSHOT);
        assertEquals(1, buffer.size());
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());

        buffer.offer(VOD_SNAPSHOT_1.getInstrumentId(), VOD_SNAPSHOT_1);
        assertEquals(2, buffer.size());
        assertFalse(buffer.isEmpty());
        assertTrue(buffer.isFull());

        buffer.poll(snapshots, 1);
        assertEquals(1, buffer.size());
        assertFalse(buffer.isEmpty());
        assertFalse(buffer.isFull());

        buffer.poll(snapshots, 1);
        assertEquals(0, buffer.size());
        assertTrue(buffer.isEmpty());
        assertFalse(buffer.isFull());
    }

    @Test
    public void shouldRejectValuesWithoutKeysWhenFull() {
        buffer = createBuffer(2);
        buffer.offer(BP_SNAPSHOT);
        buffer.offer(BP_SNAPSHOT);

        assertFalse(buffer.offer(BP_SNAPSHOT));
        assertEquals(2, buffer.size());
    }

    @Test
    public void shouldRejectNewKeysWhenFull() {
        buffer = createBuffer(2);
        buffer.offer(1L, BP_SNAPSHOT);
        buffer.offer(2L, VOD_SNAPSHOT_1);

        assertFalse(buffer.offer(4L, VOD_SNAPSHOT_2));
        assertEquals(2, buffer.size());
    }

    @Test
    public void shouldAcceptExistingKeysWhenFull() {
        buffer = createBuffer(2);
        buffer.offer(1L, BP_SNAPSHOT);
        buffer.offer(2L, BP_SNAPSHOT);

        assertTrue(buffer.offer(2L, BP_SNAPSHOT));
        assertEquals(2, buffer.size());
    }

    @Test
    public void shouldReturnSingleValue() {
        addKeyAndValue(BP_SNAPSHOT);
        assertContains(BP_SNAPSHOT);
    }

    @Test
    public void shouldReturnTwoValuesWithDifferentKeys() {
        addKeyAndValue(BP_SNAPSHOT);
        addKeyAndValue(VOD_SNAPSHOT_1);

        assertContains(BP_SNAPSHOT, VOD_SNAPSHOT_1);
    }

    @Test
    public void shouldUpdateValuesWithEqualKeys() {
        addKeyAndValue(VOD_SNAPSHOT_1);
        addKeyAndValue(VOD_SNAPSHOT_2);

        assertContains(VOD_SNAPSHOT_2);
    }

    @Test
    public void shouldNotUpdateValuesWithoutKeys() {
        addValue(VOD_SNAPSHOT_1);
        addValue(VOD_SNAPSHOT_2);

        assertContains(VOD_SNAPSHOT_1, VOD_SNAPSHOT_2);
    }

    @Test
    public void shouldUpdateValuesWithEqualKeysAndPreserveOrdering() {
        addKeyAndValue(VOD_SNAPSHOT_1);
        addKeyAndValue(BP_SNAPSHOT);
        addKeyAndValue(VOD_SNAPSHOT_2);

        assertContains(VOD_SNAPSHOT_2, BP_SNAPSHOT);
    }

    @Test
    public void shouldNotUpdateValuesIfReadOccursBetweenValues() {
        addKeyAndValue(VOD_SNAPSHOT_1);
        assertContains(VOD_SNAPSHOT_1);

        addKeyAndValue(VOD_SNAPSHOT_2);
        assertContains(VOD_SNAPSHOT_2);
    }

    @Test
    public void shouldReturnOnlyTheMaximumNumberOfRequestedItems() {
        addValue(BP_SNAPSHOT);
        addValue(VOD_SNAPSHOT_1);
        addValue(VOD_SNAPSHOT_2);

        List<MarketSnapshot> snapshots = new ArrayList<MarketSnapshot>();
        assertEquals(2, buffer.poll(snapshots, 2));
        assertEquals(2, snapshots.size());
        assertSame(BP_SNAPSHOT, snapshots.get(0));
        assertSame(VOD_SNAPSHOT_1, snapshots.get(1));

        snapshots.clear();
        assertEquals(1, buffer.poll(snapshots, 1));
        assertEquals(1, snapshots.size());
        assertSame(VOD_SNAPSHOT_2, snapshots.get(0));

        assertIsEmpty();
    }

    @Test
    public void shouldReturnAllItemsWithoutRequestLimit() {
        addValue(BP_SNAPSHOT);
        addKeyAndValue(VOD_SNAPSHOT_1);
        addKeyAndValue(VOD_SNAPSHOT_2);

        List<MarketSnapshot> snapshots = new ArrayList<MarketSnapshot>();
        assertEquals(2, buffer.poll(snapshots));
        assertEquals(2, snapshots.size());

        assertSame(BP_SNAPSHOT, snapshots.get(0));
        assertSame(VOD_SNAPSHOT_2, snapshots.get(1));

        assertIsEmpty();
    }

    @Test
    public void shouldCountRejections() throws Exception {
        CoalescingRingBuffer<Integer, Object> buffer = new CoalescingRingBuffer<Integer, Object>(2);
        assertEquals(0, buffer.rejectionCount());

        buffer.offer(new Object());
        assertEquals(0, buffer.rejectionCount());

        buffer.offer(1, new Object());
        assertEquals(0, buffer.rejectionCount());

        buffer.offer(1, new Object());
        assertEquals(0, buffer.rejectionCount());

        buffer.offer(new Object());
        assertEquals(1, buffer.rejectionCount());

        buffer.offer(2, new Object());
        assertEquals(2, buffer.rejectionCount());
    }

    @Test
    public void shouldUseObjectEqualityToCompareKeys() throws Exception {
        CoalescingRingBuffer<String, Object> buffer = new CoalescingRingBuffer<String, Object>(2);

        buffer.offer(new String("boo"), new Object());
        buffer.offer(new String("boo"), new Object());

        assertEquals(1, buffer.size());
    }

    private void addKeyAndValue(MarketSnapshot snapshot) {
        assertTrue(buffer.offer(snapshot.getInstrumentId(), snapshot));
    }

    private void addValue(MarketSnapshot snapshot) {
        assertTrue(buffer.offer(snapshot));
    }

    private void assertContains(MarketSnapshot... expected) {
        List<MarketSnapshot> actualSnapshots = new ArrayList<MarketSnapshot>(expected.length);

        int readCount = buffer.poll(actualSnapshots);
        assertSame(expected.length, readCount);

        for (int i = 0; i < expected.length; i++) {
            assertSame(expected[i], actualSnapshots.get(i));
        }

        assertTrue("buffer should now be empty", buffer.isEmpty());
    }

    private void assertIsEmpty() {
        assertContains();
    }

}
