package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.rlp.RLPLayoutable;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.signature.SignatureUtils;
import com.norswap.nanoeth.versions.EthereumVersion;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.signature.Signature;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Hashing;
import org.bouncycastle.math.ec.ECPoint;

import java.util.Arrays;
import java.util.Objects;

import static com.norswap.nanoeth.transactions.TransactionEnvelopeType.ENVELOPE_TYPE_NONE;

/**
 * A "normalized transaction", as defined in EIP-1559 (part of the {@link
 * EthereumVersion#LONDON London} hard fork).
 *
 * <p>Such a transaction can be created from any of the {@link TransactionFormat}.
 *
 * <p>Creating the transaction does not verify its signature. To do so, call {@link
 * #verifySignature()}.
 */
public final class Transaction extends UnsignedTransaction implements RLPLayoutable {

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
     * @throws RLPParsingException
     * if the RLP object does not properly parse to a transaction
     */
    public static Transaction from (RLP rlp) throws RLPParsingException {
        return TransactionParser.parse(rlp);
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
     * Returns the RLP encoding of this transaction, <b>without</b> the {@link
     * TransactionEnvelopeType} (if any). This is a sequence of transaction fields.
     *
     * @see #rlpLayout()
     */
    public RLP plainRLP() {
        return switch (format) {
            case TX_LEGACY -> RLP.sequence(
                nonce, maxFeePerGas, gasLimit, to, value, payload,
                (byte) (27 + signature.yParity), signature.r, signature.s);
            case TX_EIP_155 -> RLP.sequence(
                nonce, maxFeePerGas, gasLimit, to, value, payload,
                chainId.multiply(2).add(35).add(signature.yParity), signature.r, signature.s);
            case TX_EIP_2930 -> RLP.sequence(
                chainId, nonce, maxFeePerGas, gasLimit, to, value, payload,
                accessList, signature.yParity, signature.r, signature.s);
            case TX_EIP_1559 -> RLP.sequence(
                chainId, nonce, maxPriorityFeePerGas, maxFeePerGas, gasLimit, to, value, payload,
                accessList, signature.yParity, signature.r, signature.s);
        };
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the RLP-encoding of this transaction for inclusion in a block.
     * <p>
     * This is identical from the plain encoding generated by {@link #plainRLP()}} for
     * transactions that do not have a {@link TransactionEnvelopeType}.
     * <p>
     * For those who have an envelope type, the encoding is a RLP byte array comprising one byte
     * for the type, followed by the plain RLP encoding for the transaction.
     * <p>
     * Do not use this to obtain a binary encoding of the transaction, at it wraps typed
     * transactions in an RLP byte array. Use {@link #binary()} instead.
     */
    @Override public RLP rlpLayout() {
        var plain = plainRLP();
        if (format.type == ENVELOPE_TYPE_NONE) return plain;

        byte[] encoded = plain.encode();
        byte[] bytes = new byte[encoded.length + 1];
        bytes[0] = format.type;
        System.arraycopy(encoded, 0, bytes, 1, encoded.length);
        return RLP.bytes(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the binary encoding of this transaction, for construction of the transaction trie, or
     * transmission over the network.
     * <p>
     * The encoding is as described in {@link #rlpLayout()}, with the only difference that for typed
     * transactions, we just want the transaction type + opaque byte array representing the
     * transaction, but not the RLP size prefix.
     */
    public byte[] binary() {
        var rlp = rlpLayout();
        return format.type == ENVELOPE_TYPE_NONE
            ? rlp.encode()
            : rlp.bytes();
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
