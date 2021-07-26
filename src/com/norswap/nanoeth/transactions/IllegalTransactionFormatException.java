package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.rlp.RLP;

/**
 * Thrown when attempting to parse a RLP sequence into a transaction (or one of its components), but
 * the format of the sequence does not match what is expected.
 * 
 * @see Transaction#from(int, RLP)
 */
public final class IllegalTransactionFormatException extends Exception {

    // ---------------------------------------------------------------------------------------------

    public IllegalTransactionFormatException (String message) {
        super(message);
    }

    // ---------------------------------------------------------------------------------------------

    public IllegalTransactionFormatException (String message, Throwable cause) {
        super(message, cause);
    }

    // ---------------------------------------------------------------------------------------------
}
