package com.lmax.collections.coalescing.ring.buffer;

public interface CoalescingRingBufferViewerMBean {

    int getSize();

    int getCapacity();

    int getRemainingCapacity();

    long getRejectionCount();

    long getNextWrite();

    long getNextRead();

}