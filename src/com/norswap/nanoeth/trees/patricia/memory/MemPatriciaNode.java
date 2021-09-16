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

    /** Memoization for {@link #cap()}. */
    byte[] digest;

    // ---------------------------------------------------------------------------------------------

    // narrow the return type
    @Override public abstract MemPatriciaNode add (Nibbles keySuffix, byte[] data);

    // narrow the return type
    @Override public abstract MemPatriciaNode remove (Nibbles keySuffix);

    // ---------------------------------------------------------------------------------------------

    /**
     * This method implements the structural composition function c (equation 197 and previous in
     * the yellowpaper). See also {@link #cap()}.
     * <p>
     * This should normally never be called except from {@link #cap()}.
     */
    public abstract RLP compose();

    // ---------------------------------------------------------------------------------------------

    /**
     * This method implements the node cap function n (equation 194 in the yellowpaper).
     * <p>
     * This function takes the result of {@link #compose()} and returns its RLP encoding if its
     * size is less than 32, or a Keccak hash of the RLP encoding otherwise.
     * <p>
     * This method differs from the specified function in that, when the size is higher than 32,
     * it returns the RLP encoding of a byte array holding the hash, instead of the hash directly.
     * This makes recursion (in implementations of {@link #compose()}) more regular, but means we might
     * need to unwrap the hash in {@link #merkleRoot()}.
     * <p>
     * This method memoizes its result. This is an important optimization which avoids traversing
     * the whole tree whenever recomputing the Merkle root after a change to the tree.
     */
    public final byte[] cap() {
        if (digest != null) return digest;
        byte[] encoding = compose().encode();
        return digest = encoding.length < 32
            ? encoding
            : RLP.bytes(Hashing.keccak(encoding).bytes).encode();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public final MerkleRoot merkleRoot () {
        byte[] digest = cap();
        return RLP.encodesBytes(digest)
            ? new MerkleRoot(RLP.unwrap(digest))
            : new MerkleRoot(Hashing.keccak(digest));
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
}
