package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.data.StorageKey;
import com.norswap.nanoeth.receipts.BloomFilter;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.signature.IllegalSignature;
import com.norswap.nanoeth.signature.Signature;
import com.norswap.nanoeth.transactions.AccessList;
import com.norswap.nanoeth.transactions.AccessList.AccessListItem;
import com.norswap.nanoeth.transactions.IllegalTransactionFormatException;
import com.norswap.nanoeth.transactions.Transaction;
import com.norswap.nanoeth.transactions.TransactionFormat;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ByteUtils;
import norswap.utils.IO;
import norswap.utils.Vanilla;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.IntFunction;

import static com.norswap.nanoeth.transactions.TransactionEnvelopeType.*;
import static com.norswap.nanoeth.transactions.TransactionFormat.*;
import static com.norswap.nanoeth.utils.SharedTestsUtils.blockHeight;

/**
 * Block data from the test cases hosted at https://github.com/ethereum/tests.
 * <p>These are loaded from the {@code testdata/BlockchainTests} directory.
 */
public final class SharedBlockData {

    // ---------------------------------------------------------------------------------------------

    /** List of all parsed block test cases. */
    public static final ArrayList<BlockTestCase> TEST_CASES;

    // ---------------------------------------------------------------------------------------------

    /** The prefix (excluding the base name) of the path of the directories in which transaction
     * test cases are stored. */
    private static final String DIRECTORY_PREFIX = "testdata/BlockchainTests/ValidBlocks";

    static {
        TEST_CASES = loadTestCases();
    }

    @SuppressWarnings("ConstantConditions") // root.listFiles may return null
    private static ArrayList<BlockTestCase> loadTestCases() {
        var testCases = new ArrayList<BlockTestCase>();
        var root = new File(DIRECTORY_PREFIX);
        for (File dir: root.listFiles()) {
            if (!dir.isDirectory()) continue;
            for (File file: dir.listFiles()) {
                var fileName = file.getName();
                var string = IO.slurp(file.toString());
                var json = new JSONObject(string);

                for (var key: json.keySet()) {
                    // ignore London for now
                    if (key.endsWith("London")) continue;
                    var data = json.getJSONObject(key);
                    var split = key.split("_");
                    var version = split[split.length - 1];
                    var genesisRLP = data.getString("genesisRLP");
                    var genesisHeader = parseBlockHeader(
                            data.getJSONObject("genesisBlockHeader"));
                    var genesis = new Block(genesisHeader, new Transaction[0], new BlockHeader[0]);

                    var blocksData = data.getJSONArray("blocks");
                    var blocks = new Block[blocksData.length()];
                    for (int i = 0; i < blocksData.length(); i++)
                        blocks[i] = parseBlock(blocksData.getJSONObject(i));

                    var sealEngine = data.getString("sealEngine");
                    var validatePoW = sealEngine.equals("Ethash");
                    assert validatePoW || sealEngine.equals("NoProof");

                    testCases.add(new BlockTestCase(
                        key, blockHeight(version), genesisRLP, genesis, blocks,
                        validatePoW));
                }
            }
        }
        return testCases;
    }

    // ---------------------------------------------------------------------------------------------

    private static Block parseBlock (JSONObject block) {
        var header = parseBlockHeader(block.getJSONObject("blockHeader"));

        var transactionsJson = block.getJSONArray("transactions");
        var transactions = new Transaction[transactionsJson.length()];
        for (int i = 0; i < transactionsJson.length(); i++)
            transactions[i] = parseTransaction(transactionsJson.getJSONObject(i));

        var unclesJson = block.getJSONArray("uncleHeaders");
        var uncles = new BlockHeader[unclesJson.length()];
        for (int i = 0; i < unclesJson.length(); i++)
            uncles[i] = parseBlockHeader(unclesJson.getJSONObject(i));

        return new Block(header, transactions, uncles);
    }

    // ---------------------------------------------------------------------------------------------

    private static BlockHeader parseBlockHeader (JSONObject header) {
        var out = new BlockHeader(
            new Hash(header.getString("parentHash")),
            new Hash(header.getString("uncleHash")),
            Address.from(header.getString("coinbase")),
            new MerkleRoot(header.getString("stateRoot")),
            new MerkleRoot(header.getString("transactionsTrie")),
            new MerkleRoot(header.getString("receiptTrie")),
            new BloomFilter(header.getString("bloom")),
            new Natural(header.getString("difficulty")),
            new Natural(header.getString("number")),
            new Natural(header.getString("gasLimit")),
            new Natural(header.getString("gasUsed")),
            new Natural(header.getString("timestamp")),
            ByteUtils.hexStringToBytes(header.getString("extraData")),
            new Hash(header.getString("mixHash")),
            new Natural(header.getString("nonce")).longValue());

        var hash = new Hash(header.getString("hash"));
        Assert.that(hash.equals(out.hash()), "Supplied hash is inconsistent with block data.");

        return out;
    }

    // ---------------------------------------------------------------------------------------------

    /** See TransactionParser in transactions package for the canonical logic. */
    private static Transaction parseTransaction (JSONObject tx) {

        int type = tx.has("type")
            ? new Natural(tx.getString("type")).intValue()
            : ENVELOPE_TYPE_NONE;

        var v = new Natural(tx.getString("v"));
        int vInt = v.intValue();

        TransactionFormat format;
        try {
            format = TransactionFormat.findFormat(type, v);
        } catch (IllegalTransactionFormatException e) {
            throw new AssertionError(e);
        }

        int yParity;
        Natural chainId;
        switch (format) {
            case TX_LEGACY -> {
                yParity = vInt - 27;
                chainId = new Natural(1);
            }
            case TX_EIP_155 -> {
                yParity = (vInt - 35) % 2;
                chainId = new Natural((vInt - 35) / 2);
            }
            case TX_EIP_2930, TX_EIP_1559 -> {
                yParity = vInt;
                chainId = new Natural(tx.getString("chainId"));
            }
            default ->
                throw new AssertionError("unknown format");
        }

        try {
            return new Transaction(
                format,
                chainId,
                new Natural(tx.getString("nonce")),
                new Natural(tx.getString(format == TX_EIP_1559 ? "maxFeePerGas" : "gasPrice")),
                new Natural(tx.getString(format == TX_EIP_1559 ? "maxPriorityFeePerGas" : "gasPrice")),
                new Natural(tx.getString("gasLimit")),
                Address.from(tx.getString("to")),
                new Natural(tx.getString("value")),
                ByteUtils.hexStringToBytes(tx.getString("data")),
                parseAccessList(tx),
                new Signature(
                    yParity,
                    new Natural(tx.getString("r")),
                    new Natural(tx.getString("s"))));
        } catch (IllegalSignature e) {
            throw new Error(e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    private static AccessList parseAccessList (JSONObject tx) {
        if (!tx.has("accessList"))
            return AccessList.EMPTY;

        var itemsJson = tx.getJSONArray("accessList");
        if (itemsJson.length() == 0)
            return AccessList.EMPTY;

        var items = mapJsonObjects(itemsJson, AccessListItem[]::new,
            item -> {
                var address = new Address(item.getString("address"));
                var keysJson = item.getJSONArray("storageKeys");
                var keys = Vanilla.map(keysJson.toList(), key -> new StorageKey(((String) key)))
                    .toArray(StorageKey[]::new);
                return new AccessListItem(address, keys);
        });

        return new AccessList(items);
    }

    // ---------------------------------------------------------------------------------------------

    private static <T> T[] mapJsonObjects (
            JSONArray array, IntFunction<T[]> arrayBuilder, Function<JSONObject, T> f) {

        T[] out = arrayBuilder.apply(array.length());
        for (int i = 0; i < array.length(); i++) {
            out[i] = f.apply(array.getJSONObject(i));
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------------
}
