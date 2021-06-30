package com.norswap.nanoeth;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Bytes;
import com.norswap.nanoeth.data.Natural;

public final class Transaction {

    // ---------------------------------------------------------------------------------------------

    private final Natural nonce;
    private final Natural gasPrice;
    private final Natural gasLimit;
    private final Address to;
    private final Natural value;
    private final Bytes data;

    private final Object v, r, s; // TODO: signature of the transaction

    // ---------------------------------------------------------------------------------------------

    public Transaction (Natural nonce, Natural gasPrice, Natural gasLimit, Address to,
        Natural value, Bytes data, Object v, Object r, Object s)
    {
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.data = data;
        this.v = v;
        this.r = r;
        this.s = s;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Equal to the number of transactions sent by the sender.
     *
     * TODO: is this 1 for the first transaction?
     */
    public Natural nonce() {
        return nonce;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Number of wei to be paid per unit of gas when executing this transaction.
     *
     * <p>Yellow paper notation: Tp
     */
    public Natural gasPrice() {
        return gasPrice;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Maximum amount of gas to be used in executing this transaction.
     *
     * <p>Yellow paper notation: Tg
     */
    public Natural gasLimit() {
        return gasLimit;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * The address of the recipient of the transaction, or, for contract creation transaction,
     * the empty address.
     *
     * <p>Yellow paper notation: Tt
     */
    public Address to() {
        return to;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * The value (in Wei) to be transferred to the transaction's recipient, or as an endowment for
     * the newly created contract in the case of contract creation.
     *
     * <p>Yellow Paper notation: Tv
     */
    public Natural value() {
        return value;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the data attached to the message call, or the initialization code in case of
     * contract creation.
     */
    public Bytes data() {
        return data;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the RLP-encoded form of this transaction.
     */
    public byte[] rlp() {
        return null; // TODO
    }

    // ---------------------------------------------------------------------------------------------
}
