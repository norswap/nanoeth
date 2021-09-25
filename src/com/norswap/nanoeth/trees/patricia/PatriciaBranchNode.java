package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;

import java.util.stream.IntStream;

import static com.norswap.nanoeth.trees.patricia.AbridgedNode.Type.BRANCH;

/**
 * Abstract base classes for branch nodes.
 * <p>
 * A branch node can have some attached value, which can happen when variable-size keys are used and
 * there is a key that is a prefix of some other keys.
 * <p>
 * A branch has at least one child (if it has a child), or two children (otherwise) who differ
 * in the nibble that follows the key prefix corresponding to this branch node.
 */
public abstract class PatriciaBranchNode extends PatriciaNode {

    // ---------------------------------------------------------------------------------------------

    /** Returns the value associated with this leaf node, or null if no value is associated. */
    public abstract @Nullable byte[] value();

    // ---------------------------------------------------------------------------------------------

    /** Returns the child node with the given starting nibble, or null if there is no such child. */
    public abstract @Nullable PatriciaNode childAt (int nibble);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the cap value of {@link #childAt(int)} with parameter {@code nibble}, or null
     * if there is no such child.
     * <p>
     * This is a separate method because in the abridged representation, the children's cap values
     * are stored directly, but not the children themselves.
     */
    public abstract @Nullable byte[] childCapAt (int nibble);

    // ---------------------------------------------------------------------------------------------

    // narrow return type
    @Override public abstract PatriciaBranchNode add (KVStore store, Nibbles keySuffix, byte[] value);

    // ---------------------------------------------------------------------------------------------

    @Override public AbridgedNode abridged () {
        var childrenCaps = IntStream.range(0, 16)
            .mapToObj(this::childCapAt)
            .toArray(byte[][]::new);
        return new AbridgedNode(BRANCH, null, value(), childrenCaps);
    }

    // ---------------------------------------------------------------------------------------------
}
