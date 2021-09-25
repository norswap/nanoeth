package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.trees.patricia.NodeStore;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaExtensionNode;
import java.util.Objects;

/** An extension node in the in-memory patricia tree. */
public final class MemPatriciaExtensionNode extends PatriciaExtensionNode {

    // ---------------------------------------------------------------------------------------------

    public final Nibbles keyFragment;

    public final PatriciaBranchNode child;

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaExtensionNode (Nibbles keyFragment, PatriciaBranchNode child) {
        assert keyFragment.length() > 0 : "building extension node with empty key fragment";
        this.keyFragment = keyFragment;
        this.child = child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Nibbles keyFragment() {
        return keyFragment;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode child (NodeStore store) {
        return child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] childCap() {
        return child.cap();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof MemPatriciaExtensionNode)) return false;
        var that = (MemPatriciaExtensionNode) o;
        return keyFragment.equals(that.keyFragment) && child.equals(that.child);
    }

    @Override public int hashCode () {
        return Objects.hash(keyFragment, child);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return String.format("MemPatriciaExtensionNode{ %s = %s }", keyFragment, child);
    }

    // ---------------------------------------------------------------------------------------------
}
