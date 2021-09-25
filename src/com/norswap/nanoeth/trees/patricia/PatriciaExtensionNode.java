package com.norswap.nanoeth.trees.patricia;

import static com.norswap.nanoeth.trees.patricia.AbridgedNode.Type.EXTENSION;

/**
 * A an extension node in the patricia tree represents a shared sequence of nibbles between
 * multiples keys (a "key fragment"). This is a (yellowpaper-mandated) compression method that
 * avoids having a chain of single-child branch nodes. It can also happen that the key fragment is
 * only a single nibble-long, since we use extension nodes when there is only one child (as it is
 * more space-efficient than using a branch node — again, is is yellowpaper-mandated).
 * <p>
 * The node holds the key fragment and a child node, which is normally always a branch node.
 */
public abstract class PatriciaExtensionNode extends PatriciaNode {
    // ---------------------------------------------------------------------------------------------

    /** Returns the key fragment associated with this node. */
    public abstract Nibbles keyFragment();

    // ---------------------------------------------------------------------------------------------

    /** Returns the child node of this extension node. This should always be a branch node. */
    public abstract PatriciaBranchNode child();

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the cap value of {@link #child()}.
     * <p>
     * This is a separate method because in the abridged representation, the child's cap value
     * is stored directly, but not the child itself.
     */
    public abstract byte[] childCap();

    // ---------------------------------------------------------------------------------------------

    @Override public AbridgedNode abridged() {
        return new AbridgedNode(EXTENSION, keyFragment(), null, new byte[][]{ childCap() });
    }

    // ---------------------------------------------------------------------------------------------
}
