package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.trees.patricia.NodeStore;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import java.util.Arrays;
import java.util.Objects;

import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;

/** A branch node in the in-memory patricia tree. */
public final class MemPatriciaBranchNode extends PatriciaBranchNode {

    // ---------------------------------------------------------------------------------------------

    /** Maps the index (a nibble) to a child node, or null. */
    final PatriciaNode[] children;

    // ---------------------------------------------------------------------------------------------

    /**
     * The value held at this branch node.
     * <p>
     * Branch nodes can hold a value when variable-size keys are used and there is a key that is
     * a prefix of some other keys.
     */
    private final @Nullable byte[] value;

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaBranchNode (
            @Retained @Nullable byte[] value,
            @Retained PatriciaNode[] children) {

        this.children = children;
        this.value = value;

        assert children.length == 16
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
        return children[nibble] != null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable PatriciaNode childAt (
            NodeStore store, int nibble) {
        assert 0 <= nibble && nibble < 16;
        return children[nibble];
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable byte[] childCapAt (int nibble) {
        var child = children[nibble];
        return child == null ? null : child.cap();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof MemPatriciaBranchNode)) return false;
        var that = (MemPatriciaBranchNode) o;
        return Arrays.equals(children, that.children) && Arrays.equals(value, that.value);
    }

    @Override public int hashCode () {
        return 31 * Arrays.hashCode(children) + Objects.hashCode(value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        var b = new StringBuilder("MemPatriciaBranchNode{");
        int count = 0;
        if (value != null) {
            b.append(" self = ").append(toFullHexString(value));
            count++;
        }
        for (int i = 0; i < children.length; i++) {
            if (children[i] == null) continue;
            if (count++ != 0) b.append(",");
            b.append(String.format(" %x = %s", i, children[i]));
        }
        b.append(" }");
        return b.toString();
    }

    // ---------------------------------------------------------------------------------------------
}
