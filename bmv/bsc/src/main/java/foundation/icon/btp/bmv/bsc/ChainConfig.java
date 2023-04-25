/*
 * Copyright 2023 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package foundation.icon.btp.bmv.bsc;

import score.Context;

import java.math.BigInteger;

public class ChainConfig {
    public final long ChainID;
    public final long Epoch;
    public final long Period;
    private final long RamanujanBlock;
    private final long PlanckBlock;

    private ChainConfig(long chainId, long epoch, long period, long ramanujanBlock, long planckBlock) {
        this.ChainID = chainId;
        this.Epoch = epoch;
        this.Period = period;
        this.RamanujanBlock = ramanujanBlock;
        this.PlanckBlock = planckBlock;
    }

    public static ChainConfig fromChainID(BigInteger cid) {
        if (cid.longValue() == 56L) {
            // BSC Mainnet
            return new ChainConfig(56L, 200L, 3L, 0L, 27281024L);
        } else if (cid.longValue() == 97L) {
            // BSC Testnet
            return new ChainConfig(97L, 200L, 3L, 1010000L, 28196022L);
        } else if (cid.longValue() == 99L) {
            // Private BSC Testnet
            return new ChainConfig(99L, 200L, 3L, 0L, 0L);
        }

        Context.require(false, "No Chain Config - ChainID(" + cid.intValue() + ")");
        return null;
    }

    public boolean isEpoch(BigInteger number) {
        return number.longValue() % this.Epoch == 0;
    }

    public boolean isRamanujan(BigInteger number) {
        return number.longValue() >= this.RamanujanBlock;
    }

    public boolean isPlanck(BigInteger number) {
        return this.PlanckBlock <= number.longValue();
    }
}
