package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.annotations.Nullable;
import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.trees.patricia.AbridgedNode;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import static com.norswap.nanoeth.trees.patricia.AbridgedNode.Type.BRANCH;
import static com.norswap.nanoeth.utils.ByteUtils.toFullHexString;

/**
 * A branch node in the in-memory patricia tree. A branch has at least two children who differ
 * in the nibble that follows the key prefix corresponding to this branch node. It can also have
 * some attached value, which can happen when variable-size keys are used and there is a key that is
 * a prefix of some other keys. In that case, it can only have a single child.
 */
public final class MemPatriciaBranchNode extends MemPatriciaNode {

    // ---------------------------------------------------------------------------------------------

    /** Maps the index (a nibble) to a child node, or null. */
    public final MemPatriciaNode[] children;

    // ---------------------------------------------------------------------------------------------

    /**
     * The value held at this branch node.
     * <p>
     * Branch nodes can hold a value when variable-size keys are used and there is a key that is
     * a prefix of some other keys.
     */
    public final @Nullable byte[] value;

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaBranchNode (@Retained MemPatriciaNode[] children) {
        this(children, null);
    }

    // ---------------------------------------------------------------------------------------------

    public MemPatriciaBranchNode (
            @Retained MemPatriciaNode[] children,
            @Retained @Nullable byte[] value) {

        this.children = children;
        this.value = value;

        assert children.length == 16
            : "Merkle branch node children array should have length 16";
        assert childAndValueCount() >= 2
            : "Merkle branch node must have at least two children, or a child and a value";
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Private constructor bypassing checks in the other constructors, for the benefit of {@link
     * #insert}. Should remain package-protected.
     */
    MemPatriciaBranchNode(
            @Retained MemPatriciaNode[] children,
            @Retained @Nullable byte[] value,
            boolean marker) {
        this.children = children;
        this.value = value;
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

    @Override public MemPatriciaBranchNode add (Nibbles keySuffix, byte[] value) {
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
            ? new MemPatriciaLeafNode(suffix, value)
            // merge the current child with the new entry
            : child.add(suffix, value);
        return new MemPatriciaBranchNode(newChildren, this.value);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Return a branch node (potentially <b>mutating</b> the current one), to insert an entry with
     * the given key suffix and value, assuming the suffix shares a prefix of length {@code
     * prefixLen} with this branch node.
     * <p>
     * Because of the mutating nature of this operation it should only be used in the implementation
     * and remain package-protected.
     */
    MemPatriciaBranchNode insert (Nibbles keySuffix, byte[] value, int prefixLen) {
        if (prefixLen == keySuffix.length())
            return new MemPatriciaBranchNode(children, value, true);

        // new leaf required for the inserted entry
        var pivot  = keySuffix.get(prefixLen);
        var suffix = keySuffix.dropFirst(prefixLen + 1);
        children[pivot] = new MemPatriciaLeafNode(suffix, value);
        return this;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public MemPatriciaNode remove (Nibbles keySuffix) {
        if (keySuffix.length() == 0) // erase value
            return value == null
                ? this
                : new MemPatriciaBranchNode(children, null);

        int index  = keySuffix.get(0);
        var suffix = keySuffix.dropFirst(1);
        var newChild = children[index].remove(suffix);

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
            return new MemPatriciaLeafNode(new Nibbles(new byte[0]), value);

        for (int i = 0; i < children.length; i++) {
            // a single child is left, prepend index nibble
            if (children[i] == null || i == index) continue;
            return prepend(new Nibbles((byte) i), children[i]);
        }

        throw new Error("unreachable");
    }

    // ---------------------------------------------------------------------------------------------

    @Override public AbridgedNode abridged () {
        var childrenCaps = Arrays.stream(children)
            .map(it -> it == null ? null : it.cap())
            .toArray(byte[][]::new);
        return new AbridgedNode(BRANCH, null, value, childrenCaps);
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
