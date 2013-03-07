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
        buffer = createBuffer(16);
    }

    public CoalescingBuffer<Long, MarketSnapshot> createBuffer(int capacity) {
        return new CoalescingRingBuffer<Long, MarketSnapshot>(capacity);
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
    public void shouldRejectNonCollapsibleValueWhenFull() {
        buffer = createBuffer(2);
        buffer.offer(BP_SNAPSHOT);
        buffer.offer(BP_SNAPSHOT);

        assertFalse(buffer.offer(BP_SNAPSHOT));
        assertEquals(2, buffer.size());
    }

    @Test
    public void shouldRejectNewCollapsibleValueWhenFull() {
        buffer = createBuffer(2);
        buffer.offer(1L, BP_SNAPSHOT);
        buffer.offer(2L, VOD_SNAPSHOT_1);

        assertFalse(buffer.offer(4L, VOD_SNAPSHOT_2));
        assertEquals(2, buffer.size());
    }

    @Test
    public void shouldAcceptNewCollapsibleValueWhenFull() {
        buffer = createBuffer(2);
        buffer.offer(1L, BP_SNAPSHOT);
        buffer.offer(2L, BP_SNAPSHOT);

        assertTrue(buffer.offer(2L, BP_SNAPSHOT));
        assertEquals(2, buffer.size());
    }

    @Test
    public void shouldReturnOneUpdate() {
        addCollapsibleValue(BP_SNAPSHOT);
        assertContains(BP_SNAPSHOT);
    }

    @Test
    public void shouldReturnTwoDifferentUpdates() {
        addCollapsibleValue(BP_SNAPSHOT);
        addCollapsibleValue(VOD_SNAPSHOT_1);

        assertContains(BP_SNAPSHOT, VOD_SNAPSHOT_1);
    }

    @Test
    public void shouldCollapseTwoCollapsibleUpdatesOnSameTopic() {
        addCollapsibleValue(VOD_SNAPSHOT_1);
        addCollapsibleValue(VOD_SNAPSHOT_2);

        assertContains(VOD_SNAPSHOT_2);
    }

    @Test
    public void shouldNotCollapseTwoNonCollapsibleUpdatesOnSameTopic() {
        addNonCollapsibleValue(VOD_SNAPSHOT_1);
        addNonCollapsibleValue(VOD_SNAPSHOT_2);

        assertContains(VOD_SNAPSHOT_1, VOD_SNAPSHOT_2);
    }

    @Test
    public void shouldCollapseTwoUpdatesOnSameTopicAndPreserveOrdering() {
        addCollapsibleValue(VOD_SNAPSHOT_1);
        addCollapsibleValue(BP_SNAPSHOT);
        addCollapsibleValue(VOD_SNAPSHOT_2);

        assertContains(VOD_SNAPSHOT_2, BP_SNAPSHOT);
    }

    @Test
    public void shouldNotCollapseValuesIfReadFastEnough() {
        addCollapsibleValue(VOD_SNAPSHOT_1);
        assertContains(VOD_SNAPSHOT_1);

        addCollapsibleValue(VOD_SNAPSHOT_2);
        assertContains(VOD_SNAPSHOT_2);
    }

    @Test
    public void shouldReturnOnlyTheMaximumNumberOfRequestedItems() {
        addNonCollapsibleValue(BP_SNAPSHOT);
        addNonCollapsibleValue(VOD_SNAPSHOT_1);
        addNonCollapsibleValue(VOD_SNAPSHOT_2);

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
        addNonCollapsibleValue(BP_SNAPSHOT);
        addCollapsibleValue(VOD_SNAPSHOT_1);
        addCollapsibleValue(VOD_SNAPSHOT_2);

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

    private void addCollapsibleValue(MarketSnapshot snapshot) {
        assertTrue(buffer.offer(snapshot.getInstrumentId(), snapshot));
    }

    private void addNonCollapsibleValue(MarketSnapshot snapshot) {
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