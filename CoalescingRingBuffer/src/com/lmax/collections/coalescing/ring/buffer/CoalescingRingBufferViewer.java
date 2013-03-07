package com.lmax.collections.coalescing.ring.buffer;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class CoalescingRingBufferViewer implements CoalescingRingBufferViewerMBean {
    private final CoalescingRingBuffer<?, ?> buffer;

    public CoalescingRingBufferViewer(CoalescingRingBuffer<?, ?> buffer) {
        this.buffer = buffer;
    }

    @Override
    public int getSize() {
        return buffer.size();
    }

    @Override
    public int getCapacity() {
        return buffer.capacity();
    }

    @Override
    public int getRemainingCapacity() {
        return buffer.capacity() - buffer.size();
    }

    @Override
    public long getRejectionCount() {
        return buffer.rejectionCount();
    }

    @Override
    public long getNextWrite() {
        return buffer.nextWrite();
    }

    @Override
    public long getNextRead() {
        return buffer.nextRead();
    }

    public static void register(String bufferName, CoalescingRingBuffer<?, ?> buffer, MBeanServer mBeanServer) throws JMException {
        ObjectName name = createObjectName(bufferName);
        CoalescingRingBufferViewer bean = new CoalescingRingBufferViewer(buffer);
        mBeanServer.registerMBean(bean, name);
    }

    public static void unregister(String bufferName, MBeanServer mBeanServer) throws JMException {
        ObjectName name = createObjectName(bufferName);
        mBeanServer.unregisterMBean(name);
    }

    private static ObjectName createObjectName(String bufferName) throws MalformedObjectNameException {
        return new ObjectName("com.lmax.collections.coalescing.ring.buffer:type=" + bufferName);
    }

}