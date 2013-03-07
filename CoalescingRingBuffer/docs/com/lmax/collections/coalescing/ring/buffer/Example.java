package com.lmax.collections.coalescing.ring.buffer;

import java.lang.management.ManagementFactory;
import java.util.*;

import static java.util.Arrays.asList;

public class Example {

    public static class StockPrice {
        public final String symbol;
        public final double price;

        public StockPrice(String symbol, double price) {
            this.symbol = symbol;
            this.price = price;
        }

        @Override
        public String toString() {
            return String.format("%s =\t$%.2f", symbol, price);
        }
    }

	public class Producer extends Thread {
        private final List<String> stockSymbols = asList("FB", "RHT", "AAPL");
        private final Random random = new Random();

		public Producer() {
			super("producer");
		}

		@Override
		public void run() {
            while (true) {
                for (String symbol : stockSymbols) {
                    StockPrice price = new StockPrice(symbol, 100 * random.nextDouble());
                    boolean success = buffer.offer(symbol, price);

                    if (!success) {
                        throw new AssertionError("offer of " + symbol + " failed");
                    }
                }
            }
		}
    }

	public class Consumer extends Thread {

		public Consumer() {
			super("consumer");
		}

		@Override
		public void run() {
            List<StockPrice> prices = new ArrayList<StockPrice>(3);

            while (true) {
                buffer.poll(prices);
                for (StockPrice price : prices) {
                    System.out.println(price);
                }
                prices.clear();
            }
		}
	}

	public Example(CoalescingRingBuffer<String, StockPrice> buffer) {
		this.buffer = buffer;
	}

    private final CoalescingRingBuffer<String, StockPrice> buffer;

	public void run() throws InterruptedException {
		Producer producer = new Producer();
		Consumer consumer = new Consumer();

		producer.start();
		consumer.start();

		consumer.join();
	}

	public static void main(String[] args) throws Exception {
        CoalescingRingBuffer<String, StockPrice> buffer = new CoalescingRingBuffer<String, StockPrice>(8);

        // register an mbean to be able to view the state of the coalescing ring buffer
        CoalescingRingBufferViewer.register("Example", buffer, ManagementFactory.getPlatformMBeanServer());

        Example example = new Example(buffer);
        example.run();
	}

}