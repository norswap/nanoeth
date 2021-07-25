package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Bytes;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.rlp.RLPBytes;
import com.norswap.nanoeth.rlp.RLPSequence;
import com.norswap.nanoeth.signature.Signature;
import com.norswap.nanoeth.utils.ByteUtils;

import static com.norswap.nanoeth.transactions.TransactionFormat.*;
import static com.norswap.nanoeth.transactions.TransactionEnvelopeType.*;

/**
 * Static methods used to parse transaction from an RLP sequence.
 *
 * <p>See the {@code rlp} package for more details on RLP.
 *
 * @see Transaction#from
 */
final class TransactionParser {
    private TransactionParser() {}

    // ---------------------------------------------------------------------------------------------

    /** Implements {@link Transaction#from} */
    static Transaction parse (int type, RLPSequence seq)
            throws IllegalTransactionFormatException {

        return switch (type) {
            case ENVELOPE_TYPE_NONE     -> parseTransactionWithoutEnvelope(seq);
            case ENVELOPE_TYPE_EIP_2930 -> parseEIP2930Transaction(seq);
            case ENVELOPE_TYPE_EIP_1559 -> parseEIP1559Transaction(seq);
            default -> throw new IllegalArgumentException("invalid transaction envelope type: " + type);
        };
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseTransactionWithoutEnvelope (RLPSequence seq)
            throws IllegalTransactionFormatException  {

        var nonce = getNatural(seq, 0);
        var gasPrice = getNatural(seq, 1);
        var gasLimit = getNatural(seq, 2);
        var to = getAddress(seq, 3);
        var value = getNatural(seq, 4);
        var payload = getBytes(seq, 5);

        // See signature package README.
        TransactionFormat format;
        Natural chainId;
        int recoveryId;
        var v = getNatural(seq, 6);
        if (v.same(27) || v.same(28)) {
            format     = TX_LEGACY;
            chainId    = new Natural(1);
            recoveryId = v.intValue() - 27;
        } else if (v.greaterSame(37)) {
            format     = TX_EIP_155;
            recoveryId = v.sub(35).mod(2).intValue();
            chainId    = v.sub(35).div(2);
        } else {
            throw new IllegalTransactionFormatException("invalid v signature value");
        }

        var r = getNatural(seq, 7);
        var s = getNatural(seq, 8);
        var signature = new Signature(recoveryId, r, s); // unverified!

        return new Transaction(format, chainId, nonce, gasPrice, gasPrice, gasLimit, to, value,
            payload, AccessList.EMPTY, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseEIP2930Transaction (RLPSequence seq)
            throws IllegalTransactionFormatException {

        var chainId = getNatural(seq, 0);
        var nonce = getNatural(seq, 1);
        var gasPrice = getNatural(seq, 2);
        var gasLimit = getNatural(seq, 3);
        var to = new Address(getBytes(seq, 4).storage);
        var value = getNatural(seq, 5);
        var payload = getBytes(seq, 6);
        var accessList = getAccessList(seq, 7);
        var recoveryId = getInt(seq, 8);
        var r = getNatural(seq, 9);
        var s = getNatural(seq, 10);
        var signature = new Signature(recoveryId, r, s);

        return new Transaction(TX_EIP_2930, chainId, nonce, gasPrice, gasPrice, gasLimit, to, value,
            payload, accessList, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseEIP1559Transaction (RLPSequence seq)
            throws IllegalTransactionFormatException {

        var chainId = getNatural(seq, 0);
        var nonce = getNatural(seq, 1);
        var maxPriorityFeePerGas = getNatural(seq, 2);
        var maxFeePerGas = getNatural(seq, 3);
        var gasLimit = getNatural(seq, 4);
        var to = new Address(getBytes(seq, 5).storage);
        var value = getNatural(seq, 6);
        var payload = getBytes(seq, 7);
        var accessList = getAccessList(seq, 8);
        int recoveryId = getInt(seq, 9);
        var r = getNatural(seq, 10);
        var s = getNatural(seq, 11);
        var signature = new Signature(recoveryId, r, s);

        return new Transaction(TX_EIP_1559, chainId, nonce, maxFeePerGas, maxPriorityFeePerGas,
            gasLimit, to, value, payload, accessList, signature);
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into a natural number. */
    private static Natural getNatural (RLPSequence seq, int i)
        throws IllegalTransactionFormatException {

        return new Natural(getBytes(seq, i).storage);
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into a 32-bit integer. */
    private static int getInt (RLPSequence seq, int i)
        throws IllegalTransactionFormatException {

        return ByteUtils.toInt(getBytes(seq, i).storage);
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into an address. */
    private static Address getAddress (RLPSequence seq, int i)
            throws IllegalTransactionFormatException {

        var bytes = getBytes(seq, i);
        if (bytes.size() != 20)
            throw new IllegalTransactionFormatException("Address should be 20 bytes long");
        return new Address(bytes.storage);
    }

    // ---------------------------------------------------------------------------------------------

    /** Retrieves the i-th item of the sequence, and verifies that it is a byte array with a valid
     * size. */
    private static Bytes getBytes (RLPSequence seq, int i)
            throws IllegalTransactionFormatException {

        if (i >= seq.size())
            throw new IllegalTransactionFormatException(
                "decoded RLP for transaction is too short");

        var item = seq.get(i);

        if (!(item instanceof RLPBytes))
            throw new IllegalTransactionFormatException(
                "decoded RLP for transaction has illegal format: expected byte array at index " + i);

        return ((RLPBytes) item).bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be an RLP sequence, into an access list. */
    private static AccessList getAccessList (RLPSequence seq, int i)
            throws IllegalTransactionFormatException {
        return AccessList.from(((RLPSequence) seq.get(8)));
    }

    // ---------------------------------------------------------------------------------------------
}
