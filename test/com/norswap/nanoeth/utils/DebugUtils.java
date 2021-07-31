package com.norswap.nanoeth.utils;

import com.norswap.nanoeth.Context;
import com.norswap.nanoeth.signature.Signature;
import com.norswap.nanoeth.transactions.Transaction;

/**
 * Utilities that are useful during debugging.
 */
public final class DebugUtils {
    private DebugUtils () {}

    // ---------------------------------------------------------------------------------------------

    /**
     * Print information about the transaction to standard input, intepreting it as though
     * it occured at the given block height.
     */
    public static Transaction dump (Transaction tx, int blockHeight) {
        var oldBlock = Context.CONTEXT.blockHeight;
        Context.CONTEXT.blockHeight = blockHeight;
        var result = dump(tx);
        Context.CONTEXT.blockHeight = oldBlock;
        return result;
    }

    // ---------------------------------------------------------------------------------------------

    /** Print information about the transaction to standard input. */
    public static Transaction dump (Transaction tx) {
        System.out.println();
        System.out.println((tx.verifySignature() ? "valid " : "invalid ") + tx);

        System.out.println();
        System.out.println("computed sender: " + tx.recoverSender());
        System.out.println("computed hash: " + tx.hash());

        System.out.println();
        var pub = tx.signature.recoverPublicKey(tx.signingRLP().encode());
        System.out.println("recovered public key: " + pub);
        return tx;
    }

    // ---------------------------------------------------------------------------------------------
}
