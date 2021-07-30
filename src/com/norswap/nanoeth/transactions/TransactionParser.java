package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.signature.IllegalSignature;
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
    static Transaction parse (int type, RLP seq)
            throws IllegalTransactionFormatException {

        if (!seq.isSequence()) throw new IllegalTransactionFormatException(
            "top level RLP object is a byte array and not a sequence");

        return switch (type) {
            case ENVELOPE_TYPE_NONE     -> parseTransactionWithoutEnvelope(seq);
            case ENVELOPE_TYPE_EIP_2930 -> parseEIP2930Transaction(seq);
            case ENVELOPE_TYPE_EIP_1559 -> parseEIP1559Transaction(seq);
            default -> throw new IllegalArgumentException("invalid transaction envelope type: " + type);
        };
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseTransactionWithoutEnvelope (RLP seq)
            throws IllegalTransactionFormatException  {

        var nonce       = getNatural(seq, 0);
        var gasPrice    = getNatural(seq, 1);
        var gasLimit    = getNatural(seq, 2);
        var to          = getAddress(seq, 3);
        var value       = getNatural(seq, 4);
        var payload     = getBytes(seq, 5);

        // See signature package README.
        TransactionFormat format;
        Natural chainId;
        int yParity;
        var v = getNatural(seq, 6);
        if (v.same(27) || v.same(28)) {
            format     = TX_LEGACY;
            chainId    = new Natural(1);
            yParity    = v.intValue() - 27;
        } else if (v.greaterSame(37)) {
            format     = TX_EIP_155;
            yParity    = v.sub(35).mod(2).intValue();
            chainId    = v.sub(35).div(2);
        } else {
            throw new IllegalTransactionFormatException("invalid v signature value");
        }

        var r = getNatural(seq, 7);
        var s = getNatural(seq, 8);
        var signature = makeSignature(yParity, r, s); // legal, but unverified!

        return new Transaction(format, chainId, nonce, gasPrice, gasPrice, gasLimit, to, value,
            payload, AccessList.EMPTY, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseEIP2930Transaction (RLP seq)
            throws IllegalTransactionFormatException {

        var chainId     = getNatural(seq, 0);
        var nonce       = getNatural(seq, 1);
        var gasPrice    = getNatural(seq, 2);
        var gasLimit    = getNatural(seq, 3);
        var to          = getAddress(seq, 4);
        var value       = getNatural(seq, 5);
        var payload     = getBytes(seq, 6);
        var accessList  = getAccessList(seq, 7);
        var yParity     = getInt(seq, 8);
        var r           = getNatural(seq, 9);
        var s           = getNatural(seq, 10);
        var signature   = makeSignature(yParity, r, s); // legal, but unverified!

        return new Transaction(TX_EIP_2930, chainId, nonce, gasPrice, gasPrice, gasLimit, to, value,
            payload, accessList, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseEIP1559Transaction (RLP seq)
            throws IllegalTransactionFormatException {

        var chainId                 = getNatural(seq, 0);
        var nonce                   = getNatural(seq, 1);
        var maxPriorityFeePerGas    = getNatural(seq, 2);
        var maxFeePerGas            = getNatural(seq, 3);
        var gasLimit                = getNatural(seq, 4);
        var to                      = getAddress(seq, 5);
        var value                   = getNatural(seq, 6);
        var payload                 = getBytes(seq, 7);
        var accessList              = getAccessList(seq, 8);
        int yParity                 = getInt(seq, 9);
        var r                       = getNatural(seq, 10);
        var s                       = getNatural(seq, 11);
        var signature               = makeSignature(yParity, r, s); // legal, but unverified!

        return new Transaction(TX_EIP_1559, chainId, nonce, maxFeePerGas, maxPriorityFeePerGas,
            gasLimit, to, value, payload, accessList, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Signature makeSignature (int yParity, Natural r, Natural s)
            throws IllegalTransactionFormatException {
        try {
            return new Signature(yParity, r, s);
        } catch (IllegalSignature e) {
            throw new IllegalTransactionFormatException("illegal signature", e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses the i-th item of the sequence, which should be a byte array of size no greater than
     * 32, into a natural number.
     */
    private static Natural getNatural (RLP seq, int i)
            throws IllegalTransactionFormatException {

        byte[] bytes = getBytes(seq, i);
        if (bytes.length > 32)
            throw new IllegalTransactionFormatException(
                "Natural should not be more than 32 bytes long.");
        return new Natural(bytes);
    }
    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into a 32-bit integer. */
    private static int getInt (RLP seq, int i)
            throws IllegalTransactionFormatException {

        return ByteUtils.toInt(getBytes(seq, i));
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be a byte array, into an address. */
    private static Address getAddress (RLP seq, int i)
            throws IllegalTransactionFormatException {

        var bytes = getBytes(seq, i);
        if (bytes.length == 0)  return Address.EMPTY;
        if (bytes.length == 20) return new Address(bytes);
        throw new IllegalTransactionFormatException("Address should be 20 bytes long");
    }

    // ---------------------------------------------------------------------------------------------

    /** Retrieves the i-th item of the sequence, and verifies that it is a byte array with a valid
     * size. */
    private static byte[] getBytes (RLP seq, int i)
            throws IllegalTransactionFormatException {

        if (i >= seq.items().length) throw new IllegalTransactionFormatException(
            "decoded RLP for transaction is too short");

        var item = seq.itemAt(i);

        if (!item.isBytes()) throw new IllegalTransactionFormatException(
            "decoded RLP for transaction has illegal format: expected byte array at index " + i);

        return item.bytes();
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be an RLP sequence, into an access list. */
    private static AccessList getAccessList (RLP seq, int i)
            throws IllegalTransactionFormatException {
        // all validations are done in the `from` method
        return AccessList.from(seq.itemAt(8));
    }

    // ---------------------------------------------------------------------------------------------
}
