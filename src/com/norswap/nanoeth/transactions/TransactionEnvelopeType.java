package com.norswap.nanoeth.transactions;

/**
 * Holds constants corresponding to the identifier for the various EIP-2718 envelope types (EIP-2718
 * only introduces the envelope types mechanisms, the envelope types themselves are introduced in
 * other EIPs).
 */
public final class TransactionEnvelopeType {
    private TransactionEnvelopeType() {}

    // We can't declare these fields in TransactionFormat, as we want to use them in the enum
    // members, but the language syntax forbids both declaring static fields before the enum
    // members, and static forward references.

    // We also don't declare the envelope transaction types as an enum, as we want to use these
    // values in switch statements.

    /** Used to signify that a transaction does not have an EIP-2718 envelope type. */
    public static final int ENVELOPE_TYPE_NONE = 0;

    /** EIP-2718 envelope type introduced in EIP-2930 (access lists). */
    public static final int ENVELOPE_TYPE_EIP_2930 = 1;

    /** EIP-2718 envelope type introduced in EIP-1559 (replaces gas price by gas fee & gas tips). */
    public static final int ENVELOPE_TYPE_EIP_1559 = 2;
}