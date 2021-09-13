package com.norswap.nanoeth.trees.patricia.memory;

import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.trees.patricia.Nibbles;
import java.util.Map;
import java.util.Objects;

/**
 * This in-memory patricia tree node represents a shared sequence of nibbles between multiples keys
 * (a "key fragment"). This is a (yellowpaper-mandated) compression method that avoids having a
 * chain of single-child branch nodes. It can also happen that the key fragment is only a single
 * nibble-long, since we use extension nodes when there is only one child (as it is more
 * space-efficient than using a branch node — again, is is yellowpaper-mandated).
 * <p>
 * The node holds the key fragment and a child node, which is always a branch node.
 */
public final class MemPatriciaExtensionNode extends MemPatriciaNode {

    // ---------------------------------------------------------------------------------------------

    public final Nibbles keyFragment;

    public final MemPatriciaBranchNode child;

    public MemPatriciaExtensionNode (Nibbles keyFragment, MemPatriciaBranchNode child) {
        assert keyFragment.length() > 0 : "building extension node with empty key fragment";
        this.keyFragment = keyFragment;
        this.child = child;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] lookup (Nibbles keySuffix) {
        int len = keyFragment.length();
        return keyFragment.sharedPrefix(keySuffix) == len
            ? child.lookup(keySuffix.suffix(len))
            : null;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public MemPatriciaNode add (Nibbles keySuffix, byte[] data) {
        if (keySuffix.length() == 0) return null;
        int prefixLen = keyFragment.sharedPrefix(keySuffix);

        // the whole key fragment is shared, merge child
        if (prefixLen == keyFragment.length())
            return new MemPatriciaExtensionNode(
                keyFragment, child.add(keySuffix.suffix(prefixLen), data));

        var branch = new MemPatriciaBranchNode(new MemPatriciaNode[16], null, true);

        // If the fragment's suffix isn't empty, make an extension node wrapping the current child.
        // The branch mutation is okay, the branch hasn't escaped.
        int ownSuffixLen = keyFragment.length() - prefixLen - 1;
        var ownPivot = keyFragment.get(prefixLen);
        branch.children[ownPivot] = ownSuffixLen == 0
            ? child
            : new MemPatriciaExtensionNode(keyFragment.suffix(prefixLen + 1), child);

        branch = branch.insert(keySuffix, data, prefixLen);

        // if the shared prefix isn't empty, wrap the branch node in an extension node
        return prefixLen == 0
            ? branch
            : new MemPatriciaExtensionNode(keySuffix.prefix(prefixLen), branch);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public MemPatriciaNode remove (Nibbles keySuffix) {
        int prefixLen = keyFragment.sharedPrefix(keySuffix);
        if (prefixLen < keyFragment.length())
            return this; // not found

        var newChild = child.remove(keySuffix.suffix(prefixLen));

        if (newChild == child)
            return this; // no change
        if (newChild == null)
            return null; // if the child disappears so does the extension node
        else
            return prepend(keyFragment, newChild);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public RLP encode (SizeContext ctx) {
        SizeContext ctx2 = new SizeContext();
        RLP rlp = RLP.sequence(keyFragment.hexPrefix(false), child.encode(ctx2));

        // lower bound on RLP length (hex_prefix_encoding, child)
        // (note that single bytes can be encoded without adding a size byte)
        int fragmentLength = keyFragment.length() / 2 + 1;  // +1 for hex-prefix flags
        if (fragmentLength > 1) ++ fragmentLength;          // +1 for fragment size
        ctx2.size += fragmentLength + 1;                    // +1 for sequence size

        return cap(rlp, ctx2.size, ctx);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public void collectEntries (Nibbles prefix, Map<byte[], byte[]> map) {
        child.collectEntries(prefix.concat(keyFragment), map);
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