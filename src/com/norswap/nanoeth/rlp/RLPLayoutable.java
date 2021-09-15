package com.norswap.nanoeth.rlp;

/**
 * Implemented by object that have an RLP layout, i.e. objects for which the Ethereum specifies
 * such a layout, because it needs the RLP encoding to compute a hash, Merkle root, or to transmit
 * the object over the network.
 * <p>
 * See this package README file for more information.
 */
public interface RLPLayoutable {
    // ---------------------------------------------------------------------------------------------

    /** Returns the RLP layout for the object. */
    RLP rlpLayout();

    // ---------------------------------------------------------------------------------------------

    /** Return the RLP encoding of the object, which is {@code rlpLayout().encode()}. */
    default byte[] rlpEncode() {
        return rlpLayout().encode();
    }

    // ---------------------------------------------------------------------------------------------
}
