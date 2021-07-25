package com.norswap.nanoeth.transactions;

import static com.norswap.nanoeth.transactions.TransactionEnvelopeType.*;

/**
 * List the different possible kind of transaction that can be sent. This includes the various
 * kind of EIP-2718 envelope types (cf. {@link TransactionEnvelopeType}) as well as the original
 * and EIP-155 transactions, which predate the introduction of envelope types.
 */
public enum TransactionFormat {

    // ---------------------------------------------------------------------------------------------

    // Useful reference, transaction signature implementation in Go Ethereum:
    // https://github.com/ethereum/go-ethereum/blob/c503f98f6d5e80e079c1d8a3601d188af2a899da/core/types/transaction_signing.go

    /** Original version. Still allowed by most clients, but Geth will be deprecating it for
     * new transactions. See https://blog.ethereum.org/2021/03/03/geth-v1-10-0/#chainid-enforcement */
    TX_LEGACY (ENVELOPE_TYPE_NONE),

    /** Includes chain ID (EIP-155). */
    TX_EIP_155 (ENVELOPE_TYPE_NONE),

    /** Includes chain ID (EIP-155), access list (EIP-2930). Optional alternative. */
    TX_EIP_2930 (ENVELOPE_TYPE_EIP_2930),

    /** Includes chain ID (EIP-155), access list (EIP-2930). Replaces gas price by gas fee &
     * tips (EIP-1559). Optional alternative. */
    TX_EIP_1559 (ENVELOPE_TYPE_EIP_1559);

    /**
     * A number representing the envelope type of the transaction, or 0 if the transaction format
     * precedes the introduction of envelop types by EIP-2718.
     *
     * <p>The value of this field is one of the constants in {@link TransactionEnvelopeType}.
     */
    public final byte type;

    TransactionFormat (int type) {
        assert type <= 0x7F;
        this.type = (byte) type;
    }

    // ---------------------------------------------------------------------------------------------
}
