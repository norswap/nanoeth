package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.history.EthereumVersion;
import com.norswap.nanoeth.rlp.RLPItem;
import com.norswap.nanoeth.signature.EthKeyPair;
import com.norswap.nanoeth.signature.Signature;
import com.norswap.nanoeth.utils.ByteUtils;
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
     * if the RLP sequence does not properly encode a transaction
     */
    public static Transaction from (int type, RLPItem seq)
            throws IllegalTransactionFormatException {
        return TransactionParser.parse(type, seq);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the address of the transaction sender.
     * <p>The address is recomputed from the binary encoded transaction and the signature
     * each time this method is called.
     */
    public Address recoverSender() {
        ECPoint publicKey = Signature.recoverPublicKey(
            signature.yParity, binary(), signature.r, signature.s);
        if (publicKey == null)
            throw new IllegalStateException("The transaction's signature is invalid.");
        return EthKeyPair.address(publicKey);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the RLP form of this transaction. The byte encoding of this is what is stored in the
     * blocks, potentially prefixed by a one-byte {@link TransactionFormat#type type identifier}.
     */
    public RLPItem rlp() {
        return switch (format) {
            case TX_LEGACY -> RLPItem.sequence(
                nonce, maxFeePerGas, gasLimit, to, value, payload,
                27 + signature.yParity, signature.r, signature.s);
            case TX_EIP_155 -> RLPItem.sequence(
                nonce, maxFeePerGas, gasLimit, to, value, payload,
                chainId.mult(2).add(35).add(signature.yParity), signature.r, signature.s);
            case TX_EIP_2930 -> RLPItem.sequence(
                chainId, nonce, maxFeePerGas, gasLimit, to, value, payload,
                accessList.rlp(), signature.yParity, signature.r, signature.s);
            case TX_EIP_1559 -> RLPItem.sequence(
                chainId, nonce, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, value, payload,
                accessList.rlp(), signature.yParity, signature.r, signature.s);
        };
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the binary encoding of the transaction (which is byte-encoding of its RLP encoding).
     */
    public byte[] binary () {
        return rlp().encode();
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns an hex string (e.g. "0x123") corresponding to the {@link #binary()} encoding of this
     * transaction.
     */
    public String toHexString() {
        return ByteUtils.bytesToHexString(binary());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true if the transaction's signature is valid.
     *
     * <p>Use {@link #verifySignature(byte[])} for faster verification if you know the byte
     * encoding of the unsigned version of this transaction (which avoids recomputing it).
     */
    public boolean verifySignature() {
        return signature.verify(signingRLP().encode());
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns true if the transaction's includes a valid signature of {@code
     * encodedUnsignedTransaction} which should be the encoding of the unsigned version of this
     * transaction.
     *
     * <p>Use {@link #verifySignature()} if you do not know the encoding of the unsigned version.
     */
    public boolean verifySignature (byte[] encodedUnsignedTransaction) {
        assert Arrays.equals(signingRLP().encode(), encodedUnsignedTransaction);
        return signature.verify(encodedUnsignedTransaction);
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
            ", payload = " + Arrays.toString(payload) +
            ", accessList = " + accessList +
            ", signature = " + signature +
            '}';
    }

    // ---------------------------------------------------------------------------------------------
}
