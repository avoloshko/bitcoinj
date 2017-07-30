/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
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
import org.bitcoinj.core.Transaction;
import org.bitcoinj.script.Script;
import org.bitcoinj.utils.VersionTally;

import java.util.EnumSet;

/**
 * Parameters for the main production network on which people trade goods and services.
 */
public class MainNetParams extends LitecoinMainNetParams {
    public static final int SEGWIT_ENFORCE_HEIGHT = 481824;

    private static MainNetParams instance;
    public static synchronized MainNetParams get() {
        if (instance == null) {
            instance = new MainNetParams();
        }
        return instance;
    }

    // TODO: implement BIP-9 instead
    @Override
    public EnumSet<Script.VerifyFlag> getTransactionVerificationFlags(
            final Block block,
            final Transaction transaction,
            final VersionTally tally,
            final Integer height)
    {
        final EnumSet<Script.VerifyFlag> flags = super.getTransactionVerificationFlags(block, transaction, tally, height);
        if (height >= SEGWIT_ENFORCE_HEIGHT) flags.add(Script.VerifyFlag.SEGWIT);
        return flags;
    }

    // TODO: implement BIP-9 instead
    @Override
    public EnumSet<Block.VerifyFlag> getBlockVerificationFlags(
            final Block block,
            final VersionTally tally,
            final Integer height)
    {
        EnumSet<Block.VerifyFlag> flags = super.getBlockVerificationFlags(block, tally, height);
        if (height >= SEGWIT_ENFORCE_HEIGHT) flags.add(Block.VerifyFlag.SEGWIT);
        return flags;
    }
}
