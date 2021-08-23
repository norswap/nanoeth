package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.rlp.RLP;

/**
 * Thrown when attempting to parse a RLP sequence into a block (or one of its components), but
 * the format of the sequence does not match what is expected.
 *
 * @see Block#from(RLP)
 * @see BlockHeader#from(RLP)
 */
public final class IllegalBlockFormatException extends Exception {

    // ---------------------------------------------------------------------------------------------

    public IllegalBlockFormatException (String message) {
        super(message);
    }

    // ---------------------------------------------------------------------------------------------

    public IllegalBlockFormatException (String message, Throwable cause) {
        super(message, cause);
    }

    // ---------------------------------------------------------------------------------------------

    public IllegalBlockFormatException (Throwable cause) {
        super(cause);
    }

    // ---------------------------------------------------------------------------------------------
}
