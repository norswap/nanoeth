package com.norswap.nanoeth.transactions;

public final class OwnTransactionTestCase extends TransactionTestCase {

    // ---------------------------------------------------------------------------------------------

    public OwnTransactionTestCase (String name, int blockHeight, int chainId,
            int transactionEnvelopeType, String rlp, boolean valid) {
        super(name, blockHeight, chainId, transactionEnvelopeType, rlp, valid);
    }

    // ---------------------------------------------------------------------------------------------
}
