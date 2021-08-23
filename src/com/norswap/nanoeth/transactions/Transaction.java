package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.signature.SignatureUtils;
import com.norswap.nanoeth.versions.EthereumVersion;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.signature.Signature;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Hashing;
import org.bouncycastle.math.ec.ECPoint;

import java.util.Arrays;
import java.util.Objects;

/**
 * A "normalized transaction", as defined in EIP-1559 (part of the {@link
 * EthereumVersion#LONDON London} hard fork).
 *
 * <p>Such a transaction can be created from any of the {@link TransactionFormat}.
 *
 * <p>Creating the transaction does not verify its signature. To do so, call {@link
 * #verifySignature()}.
 */
public final class Transaction extends UnsignedTransaction {

    // ---------------------------------------------------------------------------------------------

    /** Signature of the transaction by the sender. */
    public final Signature signature;

    // ---------------------------------------------------------------------------------------------

    public Transaction (
        TransactionFormat format,
        Natural chainId,
        Natural nonce,
        Natural maxFeePerGas,
        Natural maxPriorityFeePerGas,
        Natural gasLimit,
        Address to,
        Natural value,
        byte[] payload,
        AccessList accessList,
        Signature signature)
    {
        super(format, chainId, nonce, maxFeePerGas, maxPriorityFeePerGas, gasLimit, to, value, payload, accessList);
        this.signature = signature;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses a RLP sequence into a transaction.
     *
     * @param type the EIP-2718 transaction envelope type (0 if not type). See {@link
     * TransactionFormat} for more information.
     *
     * @throws IllegalTransactionFormatException
     * if the RLP sequence does not properly parse to a transaction
     */
    public static Transaction from (int type, RLP seq)
            throws IllegalTransactionFormatException {
        return TransactionParser.parse(type, seq);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the address of the transaction sender.
     * <p>The address is recomputed from the RLP-encoded transaction and the signature
     * each time this method is called.
     */
    public Address recoverSender() {
        ECPoint publicKey = signature.recoverPublicKey(signingRLP().encode());
        if (publicKey == null)
            throw new IllegalStateException("The transaction's signature is invalid.");
        return SignatureUtils.address(publicKey);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the RLP representation of this transaction, the encoding of which is what is stored
     * in the blocks, potentially prefixed by a one-byte {@link TransactionFormat#type type
     * identifier}.
     */
    public RLP rlp() {
        return switch (format) {
            case TX_LEGACY -> RLP.sequence(
                nonce, maxFeePerGas, gasLimit, to, value, payload,
                (byte) (27 + signature.yParity), signature.r, signature.s);
            case TX_EIP_155 -> RLP.sequence(
                nonce, maxFeePerGas, gasLimit, to, value, payload,
                chainId.multiply(2).add(35).add(signature.yParity), signature.r, signature.s);
            case TX_EIP_2930 -> RLP.sequence(
                chainId, nonce, maxFeePerGas, gasLimit, to, value, payload,
                accessList.rlp(), signature.yParity, signature.r, signature.s);
            case TX_EIP_1559 -> RLP.sequence(
                chainId, nonce, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, value, payload,
                accessList.rlp(), signature.yParity, signature.r, signature.s);
        };
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the RLP encoding of the transaction (which is binary encoding of its {@link #rlp()
     * RLP representation}).
     */
    public byte[] binary () {
        return rlp().encode();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an hex string (e.g. "0x123") corresponding to the {@link #binary() RLP encoding} of
     * this transaction.
     */
    public String toHexString() {
        return ByteUtils.toCompressedHexString(binary());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true if the transaction's signature is valid.
     *
     * <p>Use {@link #verifySignature(byte[])} for faster verification if you know the RLP encoding
     * of the unsigned version of this transaction (which avoids recomputing it).
     */
    public boolean verifySignature() {
        return signature.verify(signingRLP().encode());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true if the transaction's includes a valid signature of {@code
     * encodedUnsignedTransaction} which should be the RLP encoding of the unsigned version of this
     * transaction.
     *
     * <p>Use {@link #verifySignature()} if you do not know the RLP encoding of the unsigned
     * version.
     */
    public boolean verifySignature (byte[] encodedUnsignedTransaction) {
        assert Arrays.equals(signingRLP().encode(), encodedUnsignedTransaction);
        return signature.verify(encodedUnsignedTransaction);
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the transaction hash. This is recomputed anew each time it is requested. */
    public Hash hash() {
        return Hashing.keccak(binary());
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof Transaction && super.equals(o)
            && signature.equals(((Transaction) o).signature);
    }

    @Override public int hashCode () {
        return Objects.hash(super.hashCode(), signature);
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
            ", payload = " + ByteUtils.toCompressedHexString(payload) +
            ", accessList = " + accessList +
            ", signature = " + signature +
            '}';
    }

    // ---------------------------------------------------------------------------------------------
}
