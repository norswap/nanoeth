package com.norswap.nanoeth.trees.patricia;

/** See {@link PatriciaNode#step(NodeStore, Nibbles)}. */
public final class BranchStep {
    public final PatriciaNode node;
    public final PatriciaNode child;
    public final int sharedPrefix;
    public final int nibblesLeft;

    public BranchStep (PatriciaNode node, PatriciaNode child, int sharedPrefix, int nibblesLeft) {
        this.node = node;
        this.child = child;
        this.sharedPrefix = sharedPrefix;
        this.nibblesLeft = nibblesLeft;
    }
}
