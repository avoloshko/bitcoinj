/*
 * Copyright 2013 Google Inc.
 * Copyright 2014 Andreas Schildbach
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
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Utils;
import org.bitcoinj.utils.VersionTally;

import java.util.EnumSet;

import static com.google.common.base.Preconditions.checkState;

/**
 * Parameters for the testnet, a separate public instance of Bitcoin that has relaxed rules suitable for development and testing of applications and new Bitcoin versions.
 */
public class LitecoinTestNetParams extends AbstractLitecoinNetParams {
    public static final int TESTNET_MAJORITY_WINDOW = 100; // AV??
    public static final int TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED = 75; // AV??
    public static final int TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 51; // AV??

    public LitecoinTestNetParams() {
        super();

        id = ID_TESTNET;
        interval = INTERVAL;
        targetTimespan = TARGET_TIMESPAN;

        maxTarget = Utils.decodeCompactBits(0x1e0fffffL);
        dumpedPrivateKeyHeader = 239;
        addressHeader = 111;
        p2shHeader = 196;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };

        port = 19335;
        packetMagic = 0xFDD2C8F1;
        bip32HeaderPub = 0x043587CF;
        bip32HeaderPriv = 0x04358394;

        majorityEnforceBlockUpgrade = TESTNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = TESTNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = TESTNET_MAJORITY_WINDOW;

        powAllowMinDifficultyBlocks = true;

        genesisBlock.setTime(1486949366L);
        genesisBlock.setDifficultyTarget(0x1E0FFFF0L);
        genesisBlock.setNonce(293345L);
        spendableCoinbaseDepth = 100;
        subsidyDecreaseBlockCount = 210000;

        checkpoints.put(2056, new Sha256Hash("17748a31ba97afdc9a4f86837a39d287e3e7c7290a08a1d816c5969c78a83289"));

        String genesisHash = genesisBlock.getHashAsString();
        checkState(genesisHash.equals("4966625a4b2851d9fdee139e56211a0d88575f59ed816ff5e6a63deb4e3e29a0"));

        dnsSeeds = new String[] {
            "testnet-seed.litecointools.com"
        };
    }

    private static LitecoinTestNetParams instance;
    public static synchronized LitecoinTestNetParams get() {
        if (instance == null) {
            instance = new LitecoinTestNetParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_TESTNET;
    }
}
