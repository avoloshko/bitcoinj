/*
 * Copyright 2011 Google Inc.
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

package org.bitcoinj.core;

import com.google.common.io.ByteStreams;

import org.bitcoinj.core.AbstractBlockChain.NewBlockType;
import org.bitcoinj.params.*;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.Wallet.BalanceType;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.bitcoinj.core.Utils.HEX;
import static org.junit.Assert.*;

public class BlockTest {
    private static final NetworkParameters PARAMS = TestNet2Params.get();

    public static final byte[] blockBytes;

    static {
        // Block 00000000a6e5eb79dcec11897af55e90cd571a4335383a3ccfbc12ec81085935
        // One with lots of transactions in, so a good test of the merkle tree hashing.
        blockBytes = HEX.decode("0000002043c965634f3d14a3eb05bc62386e66e232d692c5bde3732b2e3a7d17d795968b895f9eafb7b4eb2ca6d6feb9618ce3a050e258caa5f8f257568d466bb23753fb20a5c958abc3031e000ddf2a0201000000010000000000000000000000000000000000000000000000000000000000000000ffffffff1002034f0458c9a52002000000e9240000ffffffff0184860b2a010000001976a9147eb3fdb623776fb500cdb70816cf2a5256eb358e88ac0000000001000000019acb4db27f80e75e753e713cabfc1556d54db1fda667c1f53b5eeaf88dc70422000000006b4830450221009a48ff12d7cb734fc0c16055af87714ea51919f33918fc2535fa0232cbdf67d80220761d7921317d2611428a36dd656300d5df7acb7eac8d8b0bf649b4276bb998890121033bb3cad8cd13ae505862e7d3454dafe57c77a7bd5318d4eba4b161e2c5ea1d2fffffffff01aeca577b000000001976a914218983928e53944905abdbf513d66ed8c96c3c9688ac00000000");
    }

    @Before
    public void setUp() throws Exception {
        Context context = new Context(PARAMS);
    }

    @Test
    public void testWork() throws Exception {
        BigInteger work = PARAMS.getGenesisBlock().getWork();
        // This number is printed by Bitcoin Core at startup as the calculated value of chainWork on testnet:
        //
        // SetBestChain: new best=00000007199508e34a9f  height=0  work=536879104
        assertEquals(BigInteger.valueOf(1048592), work);
    }

    @Test
    public void testBlockVerification() throws Exception {
        Block block = PARAMS.getDefaultSerializer().makeBlock(blockBytes);
        block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
        assertEquals("fb7abf7c093506373fec512e25b5d8f74814b7d8b0a61c4530cfa91a34d7f0a5", block.getHashAsString());
    }
    
    @SuppressWarnings("deprecation")
    @Test
    public void testDate() throws Exception {
        Block block = PARAMS.getDefaultSerializer().makeBlock(blockBytes);
        assertEquals("15 Mar 2017 20:33:36 GMT", block.getTime().toGMTString());
    }

    @Test
    public void testProofOfWork() throws Exception {
        // This params accepts any difficulty target.
        NetworkParameters params = UnitTestParams.get();
        Block block = params.getDefaultSerializer().makeBlock(blockBytes);
        block.setNonce(12346);
        try {
            block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
            fail();
        } catch (VerificationException e) {
            // Expected.
        }
        // Blocks contain their own difficulty target. The BlockChain verification mechanism is what stops real blocks
        // from containing artificially weak difficulties.
        block.setDifficultyTarget(Block.EASIEST_DIFFICULTY_TARGET);
        // Now it should pass.
        block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
        // Break the nonce again at the lower difficulty level so we can try solving for it.
        block.setNonce(1);
        try {
            block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
            fail();
        } catch (VerificationException e) {
            // Expected to fail as the nonce is no longer correct.
        }
        // Should find an acceptable nonce.
        block.solve();
        block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
        assertEquals(block.getNonce(), 2);
    }

    @Test
    public void testBadTransactions() throws Exception {
        Block block = PARAMS.getDefaultSerializer().makeBlock(blockBytes);
        // Re-arrange so the coinbase transaction is not first.
        Transaction tx1 = block.transactions.get(0);
        Transaction tx2 = block.transactions.get(1);
        block.transactions.set(0, tx2);
        block.transactions.set(1, tx1);
        try {
            block.verify(Block.BLOCK_HEIGHT_GENESIS, EnumSet.noneOf(Block.VerifyFlag.class));
            fail();
        } catch (VerificationException e) {
            // We should get here.
        }
    }

    @Test
    public void testHeaderParse() throws Exception {
        Block block = PARAMS.getDefaultSerializer().makeBlock(blockBytes);
        Block header = block.cloneAsHeader();
        Block reparsed = PARAMS.getDefaultSerializer().makeBlock(header.bitcoinSerialize());
        assertEquals(reparsed, header);
    }

    @Test
    public void testBitcoinSerialization() throws Exception {
        // We have to be able to reserialize everything exactly as we found it for hashing to work. This test also
        // proves that transaction serialization works, along with all its subobjects like scripts and in/outpoints.
        //
        // NB: This tests the bitcoin serialization protocol.
        Block block = PARAMS.getDefaultSerializer().makeBlock(blockBytes);
        assertTrue(Arrays.equals(blockBytes, block.bitcoinSerialize()));
    }
    
    @Test
    public void testUpdateLength() {
        NetworkParameters params = UnitTestParams.get();
        Block block = params.getGenesisBlock().createNextBlockWithCoinbase(Block.BLOCK_VERSION_GENESIS, new ECKey().getPubKey(), Block.BLOCK_HEIGHT_GENESIS);
        assertEquals(block.bitcoinSerialize().length, block.length);
        final int origBlockLen = block.length;
        Transaction tx = new Transaction(params);
        // this is broken until the transaction has > 1 input + output (which is required anyway...)
        //assertTrue(tx.length == tx.bitcoinSerialize().length && tx.length == 8);
        byte[] outputScript = new byte[10];
        Arrays.fill(outputScript, (byte) ScriptOpCodes.OP_FALSE);
        tx.addOutput(new TransactionOutput(params, null, Coin.SATOSHI, outputScript));
        tx.addInput(new TransactionInput(params, null, new byte[] {(byte) ScriptOpCodes.OP_FALSE},
                new TransactionOutPoint(params, 0, Sha256Hash.of(new byte[] { 1 }))));
        int origTxLength = 8 + 2 + 8 + 1 + 10 + 40 + 1 + 1;
        assertEquals(tx.unsafeBitcoinSerialize().length, tx.length);
        assertEquals(origTxLength, tx.length);
        block.addTransaction(tx);
        assertEquals(block.unsafeBitcoinSerialize().length, block.length);
        assertEquals(origBlockLen + tx.length, block.length);
        block.getTransactions().get(1).getInputs().get(0).setScriptBytes(new byte[] {(byte) ScriptOpCodes.OP_FALSE, (byte) ScriptOpCodes.OP_FALSE});
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength + 1);
        block.getTransactions().get(1).getInputs().get(0).clearScriptBytes();
        assertEquals(block.length, block.unsafeBitcoinSerialize().length);
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength - 1);
        block.getTransactions().get(1).addInput(new TransactionInput(params, null, new byte[] {(byte) ScriptOpCodes.OP_FALSE},
                new TransactionOutPoint(params, 0, Sha256Hash.of(new byte[] { 1 }))));
        assertEquals(block.length, origBlockLen + tx.length);
        assertEquals(tx.length, origTxLength + 41); // - 1 + 40 + 1 + 1
    }

    @Test
    public void testCoinbaseHeightTestnet() throws Exception {
        // Testnet block 120768 (hash aa299f77cb859f969f470d4c48f1ad3c7cb603e5dd31a193dd2ccab1bc39da17)
        // contains a coinbase transaction whose height is two bytes, which is
        // shorter than we see in most other cases.

        Block block = TestNet3Params.get().getDefaultSerializer().makeBlock(HEX.decode("0000002088cb4be2c46a4d425b34688c37b73747de1f44cba63b0618d106256af89327a65e702bc6adabefbe3caa62be74a4d7725b2627b603dfa5e318d5280173f655e6a3ec7259811a041e9240946a0101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4c03c0d70104a3ec725908fabe6d6d0000000000000000000000000000000000000000000000000000000000000000010000000000000070000000020000000d2f6e6f64655374726174756d2f000000000100f2052a010000001976a914fbe70b337c1d2c233b46575fbf75ae9bd10c889688ac00000000"));


        // Check block.
        assertEquals("aa299f77cb859f969f470d4c48f1ad3c7cb603e5dd31a193dd2ccab1bc39da17", block.getHashAsString());
        block.verify(120768, EnumSet.of(Block.VerifyFlag.HEIGHT_IN_COINBASE));

        // Testnet block 121968 (hash 536ed2b6e33cc9bdce96f3adfcf0a09fd38a3a79a882f3e45df5aaf8963c58f2)
        // contains a coinbase transaction whose height is three bytes, but could
        // fit in two bytes. This test primarily ensures script encoding checks
        // are applied correctly.

        block = TestNet3Params.get().getDefaultSerializer().makeBlock(HEX.decode("00000020a1b6e400991e00ca691e87b9ae5655940d42be2ac7ab13d4f6efe26f1bbb55044601fe165b75a7d493fbf0f720c6f5994b91a3fa3b3474e667973e6df0d62cc4a57a73592368031e34900bac0101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff4c0370dc0104a57a735908fabe6d6d0000000000000000000000000000000000000000000000000000000000000000010000000000000067ffffff010000000d2f6e6f64655374726174756d2f000000000100f2052a010000001976a914fbe70b337c1d2c233b46575fbf75ae9bd10c889688ac00000000"));

        // Check block.
        assertEquals("536ed2b6e33cc9bdce96f3adfcf0a09fd38a3a79a882f3e45df5aaf8963c58f2", block.getHashAsString());
        block.verify(121968, EnumSet.of(Block.VerifyFlag.HEIGHT_IN_COINBASE));
    }

    @Test
    public void testReceiveCoinbaseTransaction() throws Exception {
        // Block 169482 (hash 0000000000000756935f1ee9d5987857b604046f846d3df56d024cdb5f368665)
        // contains coinbase transactions that are mining pool shares.
        // The private key MINERS_KEY is used to check transactions are received by a wallet correctly.

        // The address for this private key is 1GqtGtn4fctXuKxsVzRPSLmYWN1YioLi9y.
        final String MINING_PRIVATE_KEY = "cR4pT39YKiqZ8noBCFLaanW4r1Dg6zLpZqQGBnDfaVmY4PAtQvAM";

        final long BLOCK_NONCE = 0L;
        final Coin BALANCE_AFTER_BLOCK = Coin.valueOf(5000000000L);
        final NetworkParameters PARAMS = RegTestParams.get();

        Block block = PARAMS.getDefaultSerializer().makeBlock(HEX.decode("00000020205d3c75a8d08010d3b533ab466e330d9a9ab261150cd388673e05a2f93480acafdb2c277b2be9f42996bb0c7224a8d239a588fac4290503333d95dd1af756a63a9d7659ffff7f20000000000102000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0401170101ffffffff0200f2052a01000000232102453fdfd100e065b62312f56c0f43940be9c8db0c0111ee1df8603c4995b5b0e9ac0000000000000000266a24aa21a9ede2f61c3f71d1defd3fa999dfa36953755c690689799962b48bebd836974e8cf900000000"));

        // Check block.
        assertNotNull(block);
        block.verify(23, EnumSet.noneOf(Block.VerifyFlag.class));
        assertEquals(BLOCK_NONCE, block.getNonce());

        StoredBlock storedBlock = new StoredBlock(block, BigInteger.ONE, 169482); // Nonsense work - not used in test.

        // Create a wallet contain the miner's key that receives a spend from a coinbase.
        ECKey miningKey = DumpedPrivateKey.fromBase58(PARAMS, MINING_PRIVATE_KEY).getKey();
        assertNotNull(miningKey);
        Context context = new Context(PARAMS);
        Wallet wallet = new Wallet(context);
        wallet.importKey(miningKey);

        // Initial balance should be zero by construction.
        assertEquals(Coin.ZERO, wallet.getBalance());

        // Give the wallet the first transaction in the block - this is the coinbase tx.
        List<Transaction> transactions = block.getTransactions();
        assertNotNull(transactions);
        wallet.receiveFromBlock(transactions.get(0), storedBlock, NewBlockType.BEST_CHAIN, 0);

        // Coinbase transaction should have been received successfully but be unavailable to spend (too young).
        assertEquals(BALANCE_AFTER_BLOCK, wallet.getBalance(BalanceType.ESTIMATED));
        assertEquals(Coin.ZERO, wallet.getBalance(BalanceType.AVAILABLE));
    }

    @Test
    public void isBIPs() throws Exception {
        final MainNetParams mainnet = MainNetParams.get();
        final Block genesis = mainnet.getGenesisBlock();
        assertFalse(genesis.isBIP34());
        assertFalse(genesis.isBIP66());
        assertFalse(genesis.isBIP65());

        // 227835/00000000000001aa077d7aa84c532a4d69bdbff519609d1da0835261b7a74eb6: last version 1 block
        final Block block227835 = mainnet.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block227835.dat")));
        assertFalse(block227835.isBIP34());
        assertFalse(block227835.isBIP66());
        assertFalse(block227835.isBIP65());

        // 227836/00000000000000d0dfd4c9d588d325dce4f32c1b31b7c0064cba7025a9b9adcc: version 2 block
        final Block block227836 = mainnet.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block227836.dat")));
        assertTrue(block227836.isBIP34());
        assertFalse(block227836.isBIP66());
        assertFalse(block227836.isBIP65());

        // 363703/0000000000000000011b2a4cb91b63886ffe0d2263fd17ac5a9b902a219e0a14: version 3 block
        final Block block363703 = mainnet.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block363703.dat")));
        assertTrue(block363703.isBIP34());
        assertTrue(block363703.isBIP66());
        assertFalse(block363703.isBIP65());

        // 383616/00000000000000000aab6a2b34e979b09ca185584bd1aecf204f24d150ff55e9: version 4 block
        final Block block383616 = mainnet.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block383616.dat")));
        assertTrue(block383616.isBIP34());
        assertTrue(block383616.isBIP66());
        assertTrue(block383616.isBIP65());

        // 370661/00000000000000001416a613602d73bbe5c79170fd8f39d509896b829cf9021e: voted for BIP101
        final Block block370661 = mainnet.getDefaultSerializer()
                .makeBlock(ByteStreams.toByteArray(getClass().getResourceAsStream("block370661.dat")));
        assertTrue(block370661.isBIP34());
        assertTrue(block370661.isBIP66());
        assertTrue(block370661.isBIP65());
    }
}
