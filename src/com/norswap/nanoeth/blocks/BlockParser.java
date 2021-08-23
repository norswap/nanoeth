package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.rlp.IllegalRLPAccess;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.transactions.IllegalTransactionFormatException;
import com.norswap.nanoeth.transactions.Transaction;
import com.norswap.nanoeth.transactions.TransactionEnvelopeType;
import com.norswap.nanoeth.utils.ByteUtils;

import static com.norswap.nanoeth.rlp.RLPParsing.*;
import static com.norswap.nanoeth.transactions.TransactionEnvelopeType.ENVELOPE_TYPE_NONE;

/**
 * Static methods used to parse blocks from an RLP sequence.
 *
 * <p>See the {@code rlp} package for more details on RLP.
 *
 * @see Block#from(RLP)
 * @see BlockHeader#from(RLP)
 */
final class BlockParser {
    private BlockParser () {
    }

    // ---------------------------------------------------------------------------------------------

    static Block parseBlock (RLP rlp) throws IllegalBlockFormatException {
        int i = -1;
        try {
            var header = parseHeader(rlp.itemAt(0));

            // TODO speculative, not an execution/consensus concern, only a JSON-RPC concern
            var transactionsRLP = rlp.itemAt(1).items();
            var transactions = new Transaction[transactionsRLP.length];
            for (i = 0; i < transactions.length; i++) {
                var encodedTx = transactionsRLP[i];
                // either (a) [envelope_type, RLP_according_to_type] or (b) RLP_legacy_tx
                transactions[i] = encodedTx.items().length == 2
                    ? Transaction.from(ByteUtils.toInt(encodedTx.itemAt(0).bytes()), encodedTx.itemAt(1))
                    : Transaction.from(ENVELOPE_TYPE_NONE, encodedTx);
            }

            var unclesRLP = rlp.itemAt(2).items();
            var uncles = new BlockHeader[unclesRLP.length];
            for (i = 0; i < uncles.length; i++)
                uncles[i] = parseHeader(unclesRLP[i]);

            return new Block(header, transactions, uncles);

        } catch (IllegalRLPAccess e) {
            throw new IllegalBlockFormatException(e);
        } catch (IllegalTransactionFormatException e) {
            throw new IllegalBlockFormatException("illegal transaction at index " + i, e);
        } catch (IllegalBlockFormatException e) {
            throw new IllegalBlockFormatException(
                i == -1 ? "illegal header format" : "illegal uncle header at index " + i, e);
        }
    }

    // ---------------------------------------------------------------------------------------------

    static BlockHeader parseHeader (RLP rlp) throws IllegalBlockFormatException {
        try {
            var parentHash = getHash(rlp, 0);
            var uncleHash = getHash(rlp, 1);
            var coinbase = getAddress(rlp, 2);
            var stateRoot = getMerkleRoot(rlp, 3);
            var transactionsRoot = getMerkleRoot(rlp, 4);
            var receiptsRoot = getMerkleRoot(rlp, 5);
            var logsBloom = getBloomFilter(rlp, 6);
            var difficulty = getNatural(rlp, 7);
            var number = getNatural(rlp, 8);
            var gasLimit = getNatural(rlp, 9);
            var gasUsed = getNatural(rlp, 10);
            var timestamp = getNatural(rlp, 11);
            var extraData = getBytes(rlp, 12);
            var mixHash = getHash(rlp, 13);
            var nonce = getInt64(rlp, 14);

            return new BlockHeader(
                parentHash, uncleHash, coinbase, stateRoot, transactionsRoot, receiptsRoot, logsBloom,
                difficulty, number, gasLimit, gasUsed, timestamp, extraData, mixHash, nonce);
        } catch (IllegalRLPAccess e) {
            throw new IllegalBlockFormatException(e);
        } catch (RLPParsingException e) {
            throw new IllegalBlockFormatException(e.getMessage(), e.getCause());
        }
    }

    // ---------------------------------------------------------------------------------------------
}
