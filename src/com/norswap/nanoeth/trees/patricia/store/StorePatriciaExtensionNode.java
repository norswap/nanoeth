package com.norswap.nanoeth.trees.patricia.store;

import com.norswap.nanoeth.trees.patricia.NodeStore;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaExtensionNode;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;

/** An extension node usable for store-backed implementation, or for nodes in abridged form. */
public final class StorePatriciaExtensionNode extends PatriciaExtensionNode {

    // ---------------------------------------------------------------------------------------------

    private final Nibbles keyFragment;

    private final byte[] childCap;

    public StorePatriciaExtensionNode (Nibbles keyFragment, byte[] childCap) {
        assert keyFragment.length() > 0 : "building extension node with empty key fragment";
        this.keyFragment = keyFragment;
        this.childCap = childCap;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Nibbles keyFragment() {
        return keyFragment;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode child (NodeStore store) {
        var child = store.getNode(childCap);
        if (child == null) throw new MissingNode(childCap);
        return (PatriciaBranchNode) child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] childCap () {
        return childCap;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof StorePatriciaExtensionNode)) return false;
        var that = (StorePatriciaExtensionNode) o;
        return keyFragment.equals(that.keyFragment) && Arrays.equals(childCap, that.childCap);
    }

    @Override public int hashCode () {
        return 31 * keyFragment.hashCode() + Arrays.hashCode(childCap);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("StorePatriciaExtensionNode{ %s = %s }",
            keyFragment, ByteUtils.toFullHexString(childCap));
    }

    // ---------------------------------------------------------------------------------------------
}
