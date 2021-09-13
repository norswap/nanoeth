package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.data.MerkleRoot;

/**
 * Test case for building a modified Merkle patricia tree from a list of key-value pairs and
 * checking the resulting merkle root.
 */
public final class TrieTestCase {

    // ---------------------------------------------------------------------------------------------

    /** JSON file from which the test is extracted. */
    public final String file;

    /** Identifier naming the test in the test file. */
    public final String name;

    /**
     * Key-value pairs to be inserted (in given order), as 0x123-style hex strings.
     * <p>
     * An empty value ("0x") signifies an entry deletion.
     */
    public final String[][] pairs;

    /** The expected Merkle root for the final tree. */
    public final MerkleRoot root;

    /** Whether the keys should be hashed before insertion. */
    public final boolean isSecureTrie;

    /** Whether the key & values were originally ascii strings. */
    public final boolean asciiKeyValues;

    // ---------------------------------------------------------------------------------------------

    public TrieTestCase (String file, String name, String[][] pairs, MerkleRoot root,
            boolean isSecureTrie, boolean asciiKeyValues) {
        this.file = file;
        this.name = name;
        this.pairs = pairs;
        this.root = root;
        this.isSecureTrie = isSecureTrie;
        this.asciiKeyValues = asciiKeyValues;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString() {
        return file + "/" + name;
    }

    // ---------------------------------------------------------------------------------------------
}
