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

// deliberately mutable to make sure that thread safe does not depend on immutability
public final class MarketSnapshot {

    private long instrumentId;
    private long bestBid;
    private long bestAsk;

    public static MarketSnapshot createMarketSnapshot(long instrumentId, long bestBid, long bestAsk) {
        MarketSnapshot snapshot = new MarketSnapshot();
        snapshot.setInstrumentId(instrumentId);
        snapshot.setBestBid(bestBid);
        snapshot.setBestAsk(bestAsk);
        return snapshot;
    }

    public long getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(long instrumentId) {
        this.instrumentId = instrumentId;
    }

    public long getBid() {
        return bestBid;
    }

    public void setBestBid(long bestBid) {
        this.bestBid = bestBid;
    }

    public long getAsk() {
        return bestAsk;
    }

    public void setBestAsk(long bestAsk) {
        this.bestAsk = bestAsk;
    }

    @Override
    public String toString() {
        return instrumentId + ": " + bestBid + "/" + bestAsk;
    }
}