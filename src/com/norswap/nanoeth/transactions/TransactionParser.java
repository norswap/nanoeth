package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.signature.IllegalSignature;
import com.norswap.nanoeth.signature.Signature;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.versions.EthereumVersion;

import java.util.Arrays;

import static com.norswap.nanoeth.rlp.RLPParsing.*;
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
    static Transaction parse (RLP rlp) throws RLPParsingException {

        // The transaction can either be a RLP-encoded sequence (no envelope type), or a
        // RLP-encoded byte array whose first byte is the type, and the rest is
        // (for all currently existing types) a RLP-encoded sequence.

        final int type = rlp.isSequence()
            ? ENVELOPE_TYPE_NONE
            : ByteUtils.uint(rlp.byteAt(0));

        return switch (type) {
            case ENVELOPE_TYPE_NONE     -> parseTransactionWithoutEnvelope(rlp);
            case ENVELOPE_TYPE_EIP_2930 -> parseEIP2930Transaction(rlp);
            case ENVELOPE_TYPE_EIP_1559 -> parseEIP1559Transaction(rlp);
            default -> throw new RLPParsingException(
                    "Invalid transaction envelope type: " + type + ".");
        };
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseTransactionWithoutEnvelope (RLP seq)
            throws RLPParsingException {

        var nonce       = getNatural(seq, 0);
        var gasPrice    = getNatural(seq, 1);
        var gasLimit    = getNatural(seq, 2);
        var to          = getAddress(seq, 3);
        var value       = getNatural(seq, 4);
        var payload     = getBytes(seq, 5);

        // See signature package README.
        TransactionFormat format;
        Natural chainId;
        Natural yParity;
        var v = getNatural(seq, 6);
        if (v.same(27) || v.same(28)) {
            format     = TX_LEGACY;
            chainId    = new Natural(1);
            yParity    = v.subtract(27);
        } else if (v.greaterSame(37)) {
            if (EthereumVersion.SPURIOUS_DRAGON.isFuture())
                throw new RLPParsingException("EIP-155 transaction before Spurious Dragon");

            format     = TX_EIP_155;
            yParity    = v.subtract(35).mod(2);
            chainId    = v.subtract(35).divide(2);
        } else {
            throw new RLPParsingException("invalid v signature value");
        }

        var r = getNatural(seq, 7);
        var s = getNatural(seq, 8);
        var signature = makeSignature(yParity, r, s); // legal, but unverified!

        return new Transaction(format, chainId, nonce, gasPrice, gasPrice, gasLimit, to, value,
            payload, AccessList.EMPTY, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseEIP2930Transaction (RLP rlp) throws RLPParsingException {

        if (EthereumVersion.BERLIN.isFuture())
            throw new RLPParsingException("EIP-2930 transaction before Berlin");

        // cf. comment in parse(RLP)
        byte[] bytes = rlp.bytes();
        assert bytes[0] == ENVELOPE_TYPE_EIP_2930;
        bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        RLP seq = RLP.decode(bytes);

        var chainId     = getNatural(seq, 0);
        var nonce       = getNatural(seq, 1);
        var gasPrice    = getNatural(seq, 2);
        var gasLimit    = getNatural(seq, 3);
        var to          = getAddress(seq, 4);
        var value       = getNatural(seq, 5);
        var payload     = getBytes(seq, 6);
        var accessList  = getAccessList(seq, 7);
        var yParity     = getNatural(seq, 8);
        var r           = getNatural(seq, 9);
        var s           = getNatural(seq, 10);
        var signature   = makeSignature(yParity, r, s); // legal, but unverified!

        return new Transaction(TX_EIP_2930, chainId, nonce, gasPrice, gasPrice, gasLimit, to, value,
            payload, accessList, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Transaction parseEIP1559Transaction (RLP rlp) throws RLPParsingException {

        if (EthereumVersion.BERLIN.isFuture())
            throw new RLPParsingException("EIP-1559 transaction before London");

        // cf. comment in parse(RLP)
        byte[] bytes = rlp.bytes();
        assert bytes[0] == ENVELOPE_TYPE_EIP_2930;
        bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        RLP seq = RLP.decode(bytes);

        var chainId                 = getNatural(seq, 0);
        var nonce                   = getNatural(seq, 1);
        var maxPriorityFeePerGas    = getNatural(seq, 2);
        var maxFeePerGas            = getNatural(seq, 3);
        var gasLimit                = getNatural(seq, 4);
        var to                      = getAddress(seq, 5);
        var value                   = getNatural(seq, 6);
        var payload                 = getBytes(seq, 7);
        var accessList              = getAccessList(seq, 8);
        var yParity                 = getNatural(seq, 9);
        var r                       = getNatural(seq, 10);
        var s                       = getNatural(seq, 11);
        var signature               = makeSignature(yParity, r, s); // legal, but unverified!

        return new Transaction(TX_EIP_1559, chainId, nonce, maxFeePerGas, maxPriorityFeePerGas,
            gasLimit, to, value, payload, accessList, signature);
    }

    // ---------------------------------------------------------------------------------------------

    private static Signature makeSignature (Natural yParity, Natural r, Natural s)
            throws RLPParsingException {
        try {
            return new Signature(yParity.intValueExact(), r, s);
        } catch (IllegalSignature e) {
            throw new RLPParsingException("Illegal signature", e);
        } catch (ArithmeticException e) {
            throw new RLPParsingException(
                "y Parity value does not fit in integer: " + yParity);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** Parses the i-th item of the sequence, which should be an RLP sequence, into an access list. */
    private static AccessList getAccessList (RLP seq, int i)
            throws RLPParsingException {
        // all validations are done in the `from` method
        return AccessList.from(seq.itemAt(i));
    }

    // ---------------------------------------------------------------------------------------------
}
