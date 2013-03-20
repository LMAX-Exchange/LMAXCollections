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

package com.lmax.collections.coalescing.ring.buffer.performance;

import java.util.concurrent.CountDownLatch;

final class StopWatch {
    private final CountDownLatch startingGate = new CountDownLatch(2);
    private volatile long startTime;
    private volatile long endTime;

    void consumerIsReady() {
        awaitStart();
    }

    private void awaitStart() {
        startingGate.countDown();

        try {
            startingGate.await();
        }
        catch (InterruptedException exception) {
                throw new AssertionError(exception);
        }
    }

    void producerIsReady() {
        awaitStart();
        startTime = System.nanoTime();
    }

    void consumerIsDone() {
        endTime = System.nanoTime();
    }

    long nanosTaken() {
        return endTime - startTime;
    }
}
