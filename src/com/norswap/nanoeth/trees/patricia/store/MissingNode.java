package com.norswap.nanoeth.trees.patricia.store;

/**
 * Thrown when a node is missing from a store backing a patricia tree, i.e. when its parent has
 * recorded the cap value of the child, but the child cannot be found in the store.
 */
public final class MissingNode extends RuntimeException {

    // ---------------------------------------------------------------------------------------------

    /** Cap value of the missing node. */
    public final byte[] cap;

    // ---------------------------------------------------------------------------------------------

    public MissingNode (byte[] cap) {
        this.cap = cap;
    }

    // ---------------------------------------------------------------------------------------------
}
