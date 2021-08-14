package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.BloomFilter;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.ByteUtils;
import norswap.utils.IO;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;

public final class OfficialBlockData {

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

                String key1 = json.keys().next();
                String name = key1.split("_")[0];

                // get only Berlin for now
                if (!json.has(name + "_Berlin")) continue;
                var berlinData = json.getJSONObject(name + "_Berlin");
                var genesisRLP = berlinData.getString("genesisRLP");
                var blocks = berlinData.getJSONArray("blocks");
                for (int i = 0; i < blocks.length(); i++) {
                    var b = blocks.getJSONObject(i);
                    var h = b.getJSONObject("blockHeader");
                    var header = new BlockHeader(
                            new Hash(h.getString("parentHash")),
                            new Hash(h.getString("uncleHash")),
                            new Address(h.getString("coinbase")),
                            new MerkleRoot(h.getString("stateRoot")),
                            new MerkleRoot(h.getString("transactionsTrie")),
                            new MerkleRoot(h.getString("receiptTrie")),
                            new BloomFilter(), // TODO
                            new Natural(h.getString("difficulty")),
                            new Natural(h.getString("number")),
                            new Natural(h.getString("gasLimit")),
                            new Natural(h.getString("gasUsed")),
                            new Natural(h.getString("timestamp")),
                            ByteUtils.hexStringToBytes(h.getString("extraData")),
                            new Hash(h.getString("mixHash")),
                            new Natural(h.getString("nonce")),
                            new Hash(h.getString("hash")));
                    System.out.println(header);
                }
            }
        }
        return testCases;
    }
}
