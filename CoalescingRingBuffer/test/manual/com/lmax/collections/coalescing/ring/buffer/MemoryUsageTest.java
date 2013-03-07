package com.lmax.collections.coalescing.ring.buffer;

public class MemoryUsageTest {

    public interface ObjectFactory {
        Object makeObject();
    }

    public static long calculateMemoryUsage(ObjectFactory factory) throws InterruptedException {
        Object handle = factory.makeObject();
        long memory = usedMemory();
        handle = null;
        lotsOfGC();
        memory = usedMemory();
        handle = factory.makeObject();
        lotsOfGC();
        return usedMemory() - memory;
    }

    private static long usedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private static void lotsOfGC() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(100);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ObjectFactory factory = new ObjectFactory() {

            @Override
            public Object makeObject() {
                CoalescingRingBuffer[] buffers = new CoalescingRingBuffer[100];
                for (int i = 0; i < buffers.length; i++) {
                    buffers[i] = new CoalescingRingBuffer<Object, Object>(4096);
                }

                return buffers;
            }
        };

        long mem = calculateMemoryUsage(factory);
        System.out.println(factory.makeObject().getClass().getSimpleName() + " takes " + mem + " bytes");
    }

}
