package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.utils.ByteUtils;

/**
 * A transaction test case built from https://github.com/ethereum/tests test cases.
 */
public final class TransactionTestCase {

    // ---------------------------------------------------------------------------------------------

    public static class Result {
        public final String hash;
        public final String sender;

        public Result (String hash, String sender) {
            this.hash = hash;
            this.sender = sender;
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** The test case's file name -- we do not use the key inside the file as it is not unique. */
    public final String file;

    /** The {@link TransactionEnvelopeType transaction envelope type}. */
    public final int transactionEnvelopeType;

    /** RLP-encoding of the transaction as a {@link ByteUtils#bytesToHexString(byte[]) hex string}. */
    public final String rlp;

    /** Expected result, or null if expected to fail. */
    public final Result result;

    // ---------------------------------------------------------------------------------------------

    public TransactionTestCase (
            String file, int transactionEnvelopeType, String rlp,Result result) {

        this.file = file;
        this.transactionEnvelopeType = transactionEnvelopeType;
        this.rlp = rlp;
        this.result = result;
    }

    // ---------------------------------------------------------------------------------------------
}
