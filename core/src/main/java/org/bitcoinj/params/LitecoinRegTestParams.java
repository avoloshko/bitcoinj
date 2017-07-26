/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bitcoinj.params;

import org.bitcoinj.core.Block;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Network parameters for the regression test mode of bitcoind in which all blocks are trivially solvable.
 */
public class LitecoinRegTestParams extends AbstractLitecoinNetParams {

    private static final BigInteger MAX_TARGET = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    public LitecoinRegTestParams() {
        super();
        // Difficulty adjustments are disabled for regtest. 
        // By setting the block interval for difficulty adjustments to Integer.MAX_VALUE we make sure difficulty never changes.    

        id = ID_REGTEST;
        interval = Integer.MAX_VALUE;
        targetTimespan = TARGET_TIMESPAN;
        maxTarget = MAX_TARGET;
        dumpedPrivateKeyHeader = 239;
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };

        port = 19444;
        packetMagic = 0xfabfb5daL;
        bip32HeaderPub = 0x043587CF;
        bip32HeaderPriv = 0x04358394;

        majorityEnforceBlockUpgrade = LitecoinTestNetParams.TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = LitecoinTestNetParams.TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = LitecoinTestNetParams.TESTNET_MAJORITY_WINDOW;

        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 150;
    }

    @Override
    public boolean allowEmptyPeerChain() {
        return true;
    }

    private static Block genesis;

    @Override
    public Block getGenesisBlock() {
        synchronized (LitecoinRegTestParams.class) {
            if (genesis == null) {
                genesis = super.getGenesisBlock();
                genesis.setNonce(0);
                genesis.setDifficultyTarget(0x207fFFFFL);
                genesis.setTime(1296688602L);

                checkState(genesis.getHashAsString().toLowerCase().equals("530827f38f93b43ed12af0b3ad25a288dc02ed74d6d7857862df51fc56c416f9"));
            }
            return genesis;
        }
    }

    private static LitecoinRegTestParams instance;
    public static synchronized LitecoinRegTestParams get() {
        if (instance == null) {
            instance = new LitecoinRegTestParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_REGTEST;
    }
}
