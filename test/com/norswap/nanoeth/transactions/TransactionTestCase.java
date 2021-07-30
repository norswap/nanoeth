package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.versions.EthereumVersion;
import com.norswap.nanoeth.utils.ByteUtils;

public abstract class TransactionTestCase {
    // ---------------------------------------------------------------------------------------------

    /** The test case's name. */
    public final String name;

    /** The block height at which to evaluate the transaction.
     * Realistically, should always be a value of {@link EthereumVersion#startBlock}.
     */
    public final int blockHeight;

    /** ID for the chain the transaction belongs to. */
    public final int chainId;

    /** The {@link TransactionEnvelopeType transaction envelope type}. */
    public final int envelopeType;

    /** RLP-encoding of the transaction as a {@link ByteUtils#toFullHexString(byte[]) hex string}. */
    public final String hexRLP;

    /** Whether the transaction is valid or should be rejected. */
    public final boolean valid;

    /** Expected hex string of the hash for the transaction (if valid). Always 64 hex digit long. */
    public final String hash;

    /** Expected compressed hex string of the sender of the transaction (if valid). */
    public final String sender;

    // ---------------------------------------------------------------------------------------------

    protected TransactionTestCase (String name, int blockHeight, int chainId, int envelopeType,
            String hexRLP, boolean valid, String hash, String sender) {
        this.name = name;
        this.blockHeight = blockHeight;
        this.chainId = chainId;
        this.envelopeType = envelopeType;
        this.hexRLP = hexRLP;
        this.valid = valid;
        this.hash = hash;
        this.sender = sender;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return name;
    }


    // ---------------------------------------------------------------------------------------------
}
