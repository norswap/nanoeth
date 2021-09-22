package com.norswap.nanoeth.trees.patricia;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Used by {@link PatriciaTree#prove(byte[])} and {@link PatriciaNode#buildProof(Nibbles,
 * MerkleProofBuilder)} to build a {@link MerkleProof}.
 * <p>
 * For each node in the branch going from the root node to the leaf (all included), the {@code add}
 * method corresponding to the type of node should called on this builder class.
 */
public final class MerkleProofBuilder {
    // ---------------------------------------------------------------------------------------------

    private final byte[] key;
    private byte[] value;

    // ---------------------------------------------------------------------------------------------

    /** Amount of nodes that have been added to the builder. */
    private int length = 0;

    /** Like {@link MerkleProof#digests}, but built in reverse order. */
    private final ArrayList<byte[][]> digests = new ArrayList<>();

    /** Like {@link MerkleProof#sizes}, but built in reverse order. */
    private byte[] sizes = new byte[8];

    // ---------------------------------------------------------------------------------------------

    public MerkleProofBuilder (byte[] key) {
        this.key = key;
    }

    // ---------------------------------------------------------------------------------------------

    private void addSize (int size) {
        if (sizes.length == length)
            sizes = Arrays.copyOf(sizes, sizes.length * 2);
        sizes[length++] = (byte) size;
    }

    // ---------------------------------------------------------------------------------------------

    public void addLeafNode (Nibbles keySuffix, byte[] value) {
        digests.add(null);
        addSize(keySuffix.length());
        this.value = value;
    }

    // ---------------------------------------------------------------------------------------------

    public void addExtensionNode (Nibbles keySegment) {
        digests.add(null);
        addSize(keySegment.length());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Call this method for branch nodes where {@code data} is the value of the entry being proven.
     */
    public void addEndBranchNode (byte[] data, byte[][] digests) {
        assert digests.length == 16;
        var digestsData = Arrays.copyOf(digests, 17);
        digestsData[16] = data;
        this.digests.add(digestsData);
        addSize(0);
        this.value = data;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Call this method for branch nodes where the value for the entry being proven is in one of the
     * children of the nodes with index {@code pivot} (i.e. the value being proven is not {@code
     * data}).
     */
    public void addPathBranchNode (byte[] data, byte pivot, byte[][] digests) {
        assert digests.length == 16;
        var digestsData = Arrays.copyOf(digests, 17);
        digestsData[pivot] = new byte[0]; // mark insertion point for child
        digestsData[16] = data;
        this.digests.add(digestsData);
        addSize(1);
    }

    // ---------------------------------------------------------------------------------------------

    /** Builds and returns the proof, or returns null if there was no entry to prove. */
    public MerkleProof build() {
        if (value == null) return null;

        var proofSizes = new byte[length];
        var proofDigests = new byte[length][][];

        for (int i = 0; i < length; i++) {
            proofSizes   [i] = sizes       [length - 1 - i];
            proofDigests [i] = digests.get (length - 1 - i);
        }

        return new MerkleProof(key, value, proofSizes, proofDigests);
    }

    // ---------------------------------------------------------------------------------------------
}
