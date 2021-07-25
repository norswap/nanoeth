package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Bytes;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.history.EthereumVersion;
import com.norswap.nanoeth.rlp.RLPSequence;
import com.norswap.nanoeth.signature.EthKeyPair;
import com.norswap.nanoeth.utils.Assert;
import java.util.Objects;

/**
 * A "normalized transaction", as defined in EIP-1559 (part of the {@link
 * EthereumVersion#LONDON London} hard fork), which does not include a signature.
 *
 * <p>Such a transaction can be created from any of the {@link TransactionFormat}.
 */
public class UnsignedTransaction {

    // ---------------------------------------------------------------------------------------------

    /**
     * The original format of the transaction before normalization. This affects how the
     * transaction's signature is generated.
     */
    public final TransactionFormat format;

    // ---------------------------------------------------------------------------------------------

    /**
     * The ID of the chain on which this transaction was sent (1 for Ethereum mainnet for instance).
     *
     * <p>White paper notation: β (beta)
     */
    public final Natural chainId;

    // ---------------------------------------------------------------------------------------------

    /**
     * Sender nonce at the time the transaction was sent -- which is equal to the number of
     * transaction the sender sent before that point.
     *
     * <p>Yellow paper notation: Tn.
     */
    public final Natural nonce;

    // ---------------------------------------------------------------------------------------------

    /**
     * Max <b>total</b> amount of Wei the sender is ready to pay per unit of gas consumed.
     *
     * <p>No yellow paper notation (not updated for {@link EthereumVersion#LONDON}).
     * Replacement for the gas price (notation: Tp).
     */
    public final Natural maxFeePerGas;

    // ---------------------------------------------------------------------------------------------

    /**
     * Max amount of Wei the sender is ready to pay as a priority fee ("tip") to the miner, per unit
     * of gas consumed. This amount is included in {@link #maxFeePerGas}.
     *
     * <p>No yellow paper notation (not updated for {@link EthereumVersion#LONDON}).
     * Replacement for the gas price (notation: Tp).
     */
    public final Natural maxPriorityFeePerGas;

    // ---------------------------------------------------------------------------------------------

    /**
     * Maximum amount of gas the sender is willing to spend on this transaction.
     *
     * <p>Yellow paper notation: Tg
     */
    public final Natural gasLimit;

    // ---------------------------------------------------------------------------------------------

    /**
     * The address of the recipient of the transaction, or, for contract creation transaction,
     * the empty address.
     *
     * <p>Yellow paper notation: Tt
     */
    public final Address to;

    // ---------------------------------------------------------------------------------------------

    /**
     * The value in Wei to be transferred to the transaction's recipient, or as an endowment for the
     * newly created contract in the case of contract creation.
     *
     * <p>Yellow Paper notation: Tv
     */
    public final Natural value;

    // ---------------------------------------------------------------------------------------------

    /**
     * Either the EVM code for account initialisation when deploying a contract (yellow paper: Ti),
     * or the input data when sending a message call (yellow paper: Td).
     *
     * <p>When making a simple transfer to an EOA (externally owned account), the payload is
     * typically empty, but does not have to be. This is considered to be a message call.
     */
    public final Bytes payload;

    // ---------------------------------------------------------------------------------------------

    public final AccessList accessList;

    // ---------------------------------------------------------------------------------------------

    public UnsignedTransaction(
        TransactionFormat format,
        Natural chainId,
        Natural nonce,
        Natural maxFeePerGas,
        Natural maxPriorityFeePerGas,
        Natural gasLimit,
        Address to,
        Natural value,
        Bytes payload,
        AccessList accessList)
    {
        Assert.that(maxFeePerGas.compareTo(maxPriorityFeePerGas) >= 0,
            "Max fee per gas smaller than the max priority fee per gas.");

        this.format = format;
        this.chainId = chainId;
        this.nonce = nonce;
        this.maxFeePerGas = maxFeePerGas;
        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;
        this.payload = payload;
        this.accessList = accessList;
    }

    // ---------------------------------------------------------------------------------------------

    /** The RLP sequence to sign when signing the transaction. */
    RLPSequence signingRLP() {
        return switch (format) {
            case TX_LEGACY   ->
                RLPSequence.from(nonce, maxFeePerGas, gasLimit, to, value, payload);
            case TX_EIP_155  -> RLPSequence.from(
                // These confusing zero at the end were supposed to make it so that every field of
                // the transaction at the time was "virtually represented", with (chainId, 0, 0)
                // standing in for (v, r, s). The "why" is beyond me.
                // (inferred from https://github.com/ethereum/EIPs/commit/2cb94cc48b4466497d82f5207c84e05f8e1cf4bd)
                nonce, maxFeePerGas, gasLimit, to, value, payload, chainId, 0, 0);
            case TX_EIP_2930 -> RLPSequence.from(
                chainId, nonce, maxFeePerGas, gasLimit, to, value, payload, accessList.rlp());
            case TX_EIP_1559 -> RLPSequence.from(
                nonce, maxFeePerGas, gasLimit, to, value, payload, accessList.rlp());
        };
    }

    // ---------------------------------------------------------------------------------------------

    /** Signs the transaction using the given private key, returning the signed transaction. */
    public Transaction sign (EthKeyPair keys) {
        var signature = keys.sign(signingRLP().encode());
        return new Transaction(format, chainId, nonce, maxFeePerGas, maxPriorityFeePerGas,
            gasLimit, to, value, payload, accessList, signature);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o)
    {
        if (this == o) return true;
        if (!(o instanceof UnsignedTransaction)) return false;
        UnsignedTransaction that = (UnsignedTransaction) o;
        return chainId.equals(that.chainId)
            && format == that.format
            && nonce.equals(that.nonce)
            && maxFeePerGas.equals(that.maxFeePerGas)
            && maxPriorityFeePerGas.equals(that.maxPriorityFeePerGas)
            && gasLimit.equals(that.gasLimit)
            && to.equals(that.to)
            && value.equals(that.value)
            && payload.equals(that.payload)
            && accessList.equals(that.accessList);
    }

    @Override public int hashCode () {
        return Objects.hash(format, chainId, nonce, maxFeePerGas, maxPriorityFeePerGas, gasLimit,
            to, value, payload, accessList);
    }

    @Override public String toString () {
        return "Transaction{" +
            "format = " + format +
            ", chainId = " + chainId +
            ", nonce = " + nonce +
            ", maxFeePerGas = " + maxFeePerGas +
            ", maxPriorityFeePerGas = " + maxPriorityFeePerGas +
            ", gasLimit = " + gasLimit +
            ", to = " + to +
            ", value = " + value +
            ", payload = " + payload +
            ", accessList = " + accessList +
            '}';
    }

    // ---------------------------------------------------------------------------------------------
}