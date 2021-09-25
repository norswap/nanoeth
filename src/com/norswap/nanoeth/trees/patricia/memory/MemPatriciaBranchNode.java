package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.trees.patricia.KVStore;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import com.norswap.nanoeth.trees.patricia.PatriciaBranchNode;
import com.norswap.nanoeth.trees.patricia.PatriciaLeafNode;
import com.norswap.nanoeth.trees.patricia.PatriciaNode;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.norswap.nanoeth.trees.patricia.memory.MemPatriciaNodeUtils.prepend;
import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;

/**
 * A branch node in the in-memory patricia tree.
 */
public final class MemPatriciaBranchNode extends PatriciaBranchNode {

    // ---------------------------------------------------------------------------------------------

    /** Maps the index (a nibble) to a child node, or null. */
    private final PatriciaNode[] children;

    // ---------------------------------------------------------------------------------------------

    /**
     * The value held at this branch node.
     * <p>
     * Branch nodes can hold a value when variable-size keys are used and there is a key that is
     * a prefix of some other keys.
     */
    private final @Nullable byte[] value;

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaBranchNode (@Retained PatriciaNode[] children) {
        this(children, null);
    }

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaBranchNode (
            @Retained PatriciaNode[] children,
            @Retained @Nullable byte[] value) {

        this.children = children;
        this.value = value;

        assert children.length == 16
            : "Merkle branch node children array should have length 16";
        assert childAndValueCount() >= 2
            : "Merkle branch node must have at least two children, or a child and a value";
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Step step (Nibbles keySuffix) {
        if (keySuffix.length() == 0)
            return new Step(this, null, 0, 0);
        var child = children[keySuffix.get(0)];
        return child == null
            ? new Step(this, null, 0, keySuffix.length())
            : new Step(this, child, 1, keySuffix.length() - 1);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the number of children held by this node, + 1 if {@link #value} is set. */
    private int childAndValueCount() {
        int count = value != null ? 1 : 0;
        for (var child: children) if (child != null) ++count;
        return count;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (Nibbles keySuffix) {
        if (keySuffix.length() == 0) return value;
        var child = children[keySuffix.get(0)];
        return child != null ? child.lookup(keySuffix.dropFirst(1)) : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public MemPatriciaBranchNode add (KVStore store, Nibbles keySuffix, byte[] value) {

        if (keySuffix.length() == 0)
            // We don't clone the children, because we never mutate them,
            // and code using this shouldn't either.
            return new MemPatriciaBranchNode(children, value); // replace value

        var newChildren = children.clone();
        var pivot = keySuffix.get(0);
        var child = newChildren[pivot];
        var suffix = keySuffix.dropFirst(1);
        newChildren[pivot] = child == null
            // no child currently starting with the first nibble of the suffix: create new leaf
            ? new PatriciaLeafNode(suffix, value)
            // merge the current child with the new entry
            : child.add(store, suffix, value);
        return new MemPatriciaBranchNode(newChildren, this.value);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode remove (KVStore store,
            Nibbles keySuffix) {
        if (keySuffix.length() == 0) // erase value
            return value == null
                ? this
                : new MemPatriciaBranchNode(children, null);

        int index  = keySuffix.get(0);
        var suffix = keySuffix.dropFirst(1);
        var newChild = children[index].remove(store, suffix);

        if (newChild == children[index])
            return this; // no change

        if (newChild != null) { // replace child
            var newChildren = children.clone();
            newChildren[index] = newChild;
            return new MemPatriciaBranchNode(newChildren, value);
        }

        // child was deleted
        int newCount = childAndValueCount() - 1;

        if (newCount >= 2) {
            // simply remove the deleted child
            var newChildren = children.clone();
            newChildren[index] = null;
            return new MemPatriciaBranchNode(newChildren, value);
        }

        if (value != null)
            // only the data is left, make this a leaf node
            return new PatriciaLeafNode(Nibbles.EMPTY, value);

        for (int i = 0; i < children.length; i++) {
            // a single child is left, prepend index nibble
            if (children[i] == null || i == index) continue;
            return prepend(new Nibbles((byte) i), children[i]);
        }

        throw new Error("unreachable");
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable byte[] value() {
        return value;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable PatriciaNode childAt (int nibble) {
        assert 0 <= nibble && nibble < 16;
        return children[nibble];
    }

    // ---------------------------------------------------------------------------------------------

    @Override public @Nullable byte[] childCapAt (int nibble) {
        var child = childAt(nibble);
        return child == null ? null : child.cap();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
        if (value != null)
            map.put(prefix.bytes(), value);

        for (int i = 0; i < children.length; i++) {
            if (children[i] == null) continue;
            children[i].collectEntries(prefix.concat(new Nibbles((byte) i)), map);
        }
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
