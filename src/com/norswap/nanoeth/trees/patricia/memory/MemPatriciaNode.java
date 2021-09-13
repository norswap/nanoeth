package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import com.norswap.nanoeth.utils.Hashing;

/**
 * The common interface for all in-memory patricia tree node implementations.
 */
public abstract class MemPatriciaNode implements PatriciaNode {

    // ---------------------------------------------------------------------------------------------

    // narrow the return type
    @Override public abstract MemPatriciaNode add (Nibbles keySuffix, byte[] data);

    // narrow the return type
    @Override public abstract MemPatriciaNode remove (Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * Passed to {@link #encode} so that the caller (the parent node) may track the RLP-encoded
     * size of its children without actually needing to call {@link RLP#encode()}.
     * <p>
     * The size needs to be accurate if smaller than 32, but may be off (in practice: lower than the
     * actual size) if larger than 32. This is because we only really care to know whether the
     * encoding is smaller than 32 bytes for compression purposes.
     */
    public static final class SizeContext {
        int size = 0;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * The implementation of this method together implement the node cap function n and the
     * composition function c (equation 197 and previous in the yellowpaper). The return value of
     * this function is the return of the n function.
     * <p>
     * We implement c and n together, because recursion is always done through n (which then calls
     * c). The only time we call c directly is when calling TRIE (the function that returns the
     * Merkle root). In practice this means that this function must keep track of the size of its
     * RLP encoding and return a RLP-encoded hash instead if the size exceeds 32 bytes. This also
     * means that {@link #merkleRoot()} (which implements the yellowpaper TRIE function) must unwrap
     * the hash in those cases (to avoid double hashing).
     *
     * @param ctx must be incremented with the size of the binary encoding of the returned RLP
     * object (see {@link SizeContext} for more details).
     */
    public abstract RLP encode (SizeContext ctx);

    // ---------------------------------------------------------------------------------------------

    @Override public final MerkleRoot merkleRoot () {
        var rlp = encode(new SizeContext());
        // Because encode() is the n function, we need to avoid rehashing a hash.
        return rlp.isBytes()
            ? new MerkleRoot(rlp.bytes())
            : new MerkleRoot(Hashing.keccak(rlp.encode()));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a new node that corresponds to the node that the given nibbles prepended.
     * This results in a single node of the same type for leaf and extension nodes, and for
     * an extension node holding the original node for branch nodes.
     * <p>
     * This is used by branch and extension nodes to implement entry deletion.
     */
    MemPatriciaNode prepend (Nibbles nibbles, MemPatriciaNode node) {
        if (node instanceof MemPatriciaExtensionNode) {
            var extNode = (MemPatriciaExtensionNode) node;
            return new MemPatriciaExtensionNode(
                nibbles.concat(extNode.keyFragment), extNode.child);
        }
        if (node instanceof MemPatriciaLeafNode) {
            var leafNode = (MemPatriciaLeafNode) node;
            return new MemPatriciaLeafNode(
                nibbles.concat(leafNode.keySuffix), leafNode.data);
        }
        if  (node instanceof MemPatriciaBranchNode) {
            var branchNode = (MemPatriciaBranchNode) node;
            return new MemPatriciaExtensionNode(nibbles, branchNode);
        }
        throw new Error(); // unreachable
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Implements the node cap function n (equation 196 in the yellowpaper).
     * <p>
     * In practice: returns {@code rlp} if its size is less than 32, and its hash otherwise. An
     * important difference to the yellowpaper is that if a hash is returned, it is wrapped in an
     * RLP byte sequence.
     */
    RLP cap (RLP rlp, int rlpSizeEstimation, SizeContext ctx) {
        assert rlpSizeEstimationIsCorrect(rlpSizeEstimation, rlp);
        if (rlpSizeEstimation < 32) {
            ctx.size += rlpSizeEstimation;
            return rlp;
        } else {
            ctx.size += 32;
            return RLP.bytes(Hashing.keccak(rlp.encode()).bytes);
        }
    }

    // ---------------------------------------------------------------------------------------------

    private boolean rlpSizeEstimationIsCorrect (int size, RLP rlp) {
        int len = rlp.encode().length;
        return size < 32  && size == len
            || size >= 32 && size <= len;
    }

    // ---------------------------------------------------------------------------------------------
}
