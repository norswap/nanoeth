package com.norswap.nanoeth.trees.patricia.store;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.trees.patricia.NodeStore;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import java.util.Arrays;

import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;

/** A branch node usable for store-backed implementation, or for nodes in abridged form. */
public final class StorePatriciaBranchNode extends PatriciaBranchNode {

    // ---------------------------------------------------------------------------------------------

    /** The value associated with this node (or null). */
    public final @Nullable byte[] value;

    // ---------------------------------------------------------------------------------------------

    /**
     * The cap values (i.e. result of the yellowpaper's node cap function n - equation 196) of all
     * children of this node. Must have size 16.
     * <p>
     * For empty slots in branch nodes, {@code null} is used.
     */
    public final @Nullable byte[][] childrenCaps;

    // ---------------------------------------------------------------------------------------------

    public StorePatriciaBranchNode (byte[] value, byte[][] childrenCaps) {
        this.value = value;
        this.childrenCaps = childrenCaps;

        assert childrenCaps.length == 16
            : "Merkle branch node children array should have length 16";
        assert childAndValueCount() >= 2
            : "Merkle branch node must have at least two children, or a child and a value";
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable byte[] value() {
        return value;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean hasChildAt (int nibble) {
        assert 0 <= nibble && nibble < 16;
        return childrenCaps[nibble] != null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable PatriciaNode childAt (NodeStore store, int nibble) {
        assert 0 <= nibble && nibble < 16;
        var cap = childCapAt(nibble);
        if (cap == null)
            return null;
        var child = store.getNode(cap);
        if (child == null)
            throw new MissingNode(cap);
        return child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable byte[] childCapAt (int nibble) {
        assert 0 <= nibble && nibble < 16;
        return childrenCaps[nibble];
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof StorePatriciaBranchNode)) return false;
        var that = (StorePatriciaBranchNode) o;
        return Arrays.equals(value, that.value)
            && Arrays.deepEquals(childrenCaps, that.childrenCaps);
    }

    @Override public int hashCode () {
        return 31 * Arrays.hashCode(value) + Arrays.deepHashCode(childrenCaps);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        var b = new StringBuilder("StorePatriciaBranchNode{");
        int count = 0;
        if (value != null) {
            b.append(" self = ").append(toFullHexString(value));
            count++;
        }
        for (int i = 0; i < childrenCaps.length; i++) {
            if (childrenCaps[i] == null) continue;
            if (count++ != 0) b.append(",");
            b.append(String.format(" %x = %s", i, toFullHexString(childrenCaps[i])));
        }
        b.append(" }");
        return b.toString();
    }

    // ---------------------------------------------------------------------------------------------
}
