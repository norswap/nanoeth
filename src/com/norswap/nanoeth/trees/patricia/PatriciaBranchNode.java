package com.norswap.nanoeth.trees.patricia;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.rlp.RLP;

import java.util.Arrays;
import java.util.Map;

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

    private final static RLP EMPTY_BYTE_ARRAY = RLP.bytes(new byte[0]);

    // ---------------------------------------------------------------------------------------------

    /** Returns the value associated with this leaf node, or null if no value is associated. */
    public abstract @Nullable byte[] value();

    // ---------------------------------------------------------------------------------------------

    /** Returns true only if the branch node has a child with the given starting nibble. */
    public abstract boolean hasChildAt (int nibble);

    // ---------------------------------------------------------------------------------------------

    /** Returns the child node with the given starting nibble, or null if there is no such child. */
    public abstract @Nullable PatriciaNode childAt (NodeStore store, int nibble);

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the cap value of {@link #childAt(NodeStore, int)} with parameter {@code nibble}, or null
     * if there is no such child.
     * <p>
     * This is a separate method because in the abridged representation, the children's cap values
     * are stored directly, but not the children themselves.
     */
    public abstract @Nullable byte[] childCapAt (int nibble);

    // ---------------------------------------------------------------------------------------------

    /** Returns the number of children held by this node, + 1 if {@link #value} is set. */
    public int childAndValueCount() {
        int count = value() != null ? 1 : 0;
        for (int i = 0; i < 16; i++)
            if (hasChildAt(i)) ++count;
        return count;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public final BranchStep step (NodeStore store, Nibbles keySuffix) {
        if (keySuffix.length() == 0)
            return new BranchStep(this, null, 0, 0);
        var child = childAt(store, keySuffix.get(0));
        return child == null
            ? new BranchStep(this, null, 0, keySuffix.length())
            : new BranchStep(this, child, 1, keySuffix.length() - 1);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (NodeStore store, Nibbles keySuffix) {
        if (keySuffix.length() == 0) return value();
        var child = childAt(store, keySuffix.get(0));
        return child != null ? child.lookup(store, keySuffix.dropFirst(1)) : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaBranchNode add (NodeStore store, Nibbles keySuffix, byte[] value) {
        if (keySuffix.length() == 0)
            return Arrays.equals(this.value(), value)
                ? this // no change
                : store.withValue(this, value); // replace value

        var pivot = keySuffix.get(0);
        var child = childAt(store, pivot);
        var suffix = keySuffix.dropFirst(1);
        var newChild = child == null
            // no child currently starting with the first nibble of the suffix: create new leaf
            ? store.leafNode(suffix, value)
            // merge the current child with the new entry
            : child.add(store, suffix, value);
        return store.withChild(this, pivot, newChild);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public PatriciaNode remove (NodeStore store, Nibbles keySuffix) {

        if (keySuffix.length() == 0) // erase value
            return value() == null
                ? this
                : store.withValue(this, null);

        int index  = keySuffix.get(0);
        var suffix = keySuffix.dropFirst(1);
        var oldChild = childAt(store, index);
        var newChild = oldChild.remove(store, suffix);

        if (newChild == oldChild)
            return this; // no change

        if (newChild != null) // replace child
            return store.withChild(this, index, newChild);

        // child was deleted
        int newCount = childAndValueCount() - 1;

        if (newCount >= 2)
            // simply remove the deleted child
            return store.withChild(this, index, null);

        if (value() != null) {
            // only the data is left, make this a leaf node
            store.removeNode(this);
            return store.leafNode(Nibbles.EMPTY, value());
        }

        for (int i = 0; i < 16; i++) {
            // a single child is left, prepend index nibble
            if (!hasChildAt(i) || i == index) continue;
            return store.prepend(new Nibbles((byte) i), childAt(store, i));
        }

        throw new Error("unreachable");
    }

    // ---------------------------------------------------------------------------------------------

    @Override public final RLP compose() {
        var sequence = new Object[17];
        for (int i = 0; i < 16; i++) {
            var childCap = childCapAt(i) ;
            sequence[i] = childCap == null
                    ? EMPTY_BYTE_ARRAY
                    : wrappedCap(childCap);
        }
        sequence[16] = value() == null ? EMPTY_BYTE_ARRAY : value();
        return RLP.sequence(sequence);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (NodeStore store, Nibbles prefix, Map<byte[], byte[]> map) {
        if (value() != null)
            map.put(prefix.bytes(), value());

        for (int i = 0; i < 16; i++) {
            var child = childAt(store, i);
            if (child == null) continue;
            child.collectEntries(store, prefix.concat(new Nibbles((byte) i)), map);
        }
    }

    // ---------------------------------------------------------------------------------------------
}
