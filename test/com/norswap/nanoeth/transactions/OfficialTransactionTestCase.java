package com.norswap.nanoeth.transactions;

/**
 * A transaction test case built from https://github.com/ethereum/tests test cases.
 */
public final class OfficialTransactionTestCase extends TransactionTestCase {

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

    /** Expected result, or null if expected to fail. */
    public final Result result;

    // ---------------------------------------------------------------------------------------------

    /**
     * For the name, must use the test case's file name, as the key inside the file is not unique.
     */
    public OfficialTransactionTestCase (
            String file, int blockHeight, String rlp, Result result) {
        super(file, blockHeight, /* chainId */ 1, /* envelopeType */ 0, rlp, result != null);
        this.result = result;
    }

    // ---------------------------------------------------------------------------------------------
}
