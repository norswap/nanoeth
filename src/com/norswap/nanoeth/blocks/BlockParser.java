package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.receipts.BloomFilter;
import com.norswap.nanoeth.rlp.IllegalRLPAccess;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.transactions.Transaction;
import static com.norswap.nanoeth.rlp.RLPParsing.*;

/**
 * Static methods used to parse blocks from an RLP sequence.
 *
 * <p>See the {@code rlp} package for more details on RLP.
 *
 * @see Block#from(RLP)
 * @see BlockHeader#from(RLP)
 */
final class BlockParser {
    private BlockParser () {}

    // ---------------------------------------------------------------------------------------------

    /** Implements {@link Block#from} */
    static Block parseBlock (RLP rlp) throws RLPParsingException {
        int i = -1, j = -1;
        try {
            var header = parseHeader(rlp.itemAt(0));

            var transactionsRLP = rlp.itemAt(1).items();
            var transactions = new Transaction[transactionsRLP.length];
            for (i = 0; i < transactions.length; i++)
                transactions[i] = Transaction.from(transactionsRLP[i]);

            var unclesRLP = rlp.itemAt(2).items();
            var uncles = new BlockHeader[unclesRLP.length];
            for (j = 0; j < uncles.length; j++)
                uncles[j] = parseHeader(unclesRLP[j]);

            return new Block(header, transactions, uncles);

        } catch (IllegalRLPAccess e) {
            throw new RLPParsingException(e.getMessage(), e);
        } catch (RLPParsingException e) {
            if (i == -1 || j >= 0) // header
                e.trace.pop();

            if      (i == -1) e.trace.push("Illegal header format.");
            else if (j == -1) e.trace.push("Illegal transaction at index " + i + ".");
            else              e.trace.push("Illegal uncle header at index " + j + ".");
            throw e;
        }
    }

    // ---------------------------------------------------------------------------------------------

    static BlockHeader parseHeader (RLP rlp) throws RLPParsingException {
        try {
            var parentHash          = getHash           (rlp, 0);
            var uncleHash           = getHash           (rlp, 1);
            var coinbase            = getAddress        (rlp, 2);
            var stateRoot           = getMerkleRoot     (rlp, 3);
            var transactionsRoot    = getMerkleRoot     (rlp, 4);
            var receiptsRoot        = getMerkleRoot     (rlp, 5);
            var logsBloom           = getBloomFilter    (rlp, 6);
            var difficulty          = getNatural        (rlp, 7);
            var number              = getNatural        (rlp, 8);
            var gasLimit            = getNatural        (rlp, 9);
            var gasUsed             = getNatural        (rlp, 10);
            var timestamp           = getNatural        (rlp, 11);
            var extraData           = getBytes          (rlp, 12);
            var mixHash             = getHash           (rlp, 13);
            var nonce               = getInt64          (rlp, 14);

            return new BlockHeader(
                parentHash, uncleHash, coinbase, stateRoot, transactionsRoot, receiptsRoot, logsBloom,
                difficulty, number, gasLimit, gasUsed, timestamp, extraData, mixHash, nonce);

        } catch (IllegalRLPAccess e) {
            throw new RLPParsingException(e.getMessage(), e);
        } catch (RLPParsingException e) {
            e.trace.push("Illegal header format.");
            throw e;
        }
    }

    // ---------------------------------------------------------------------------------------------

    public static Hash getHash (RLP seq, int i) throws RLPParsingException {
        return Hash.parse(seq.itemAt(i));
    }

    // ---------------------------------------------------------------------------------------------

    public static MerkleRoot getMerkleRoot (RLP seq, int i) throws RLPParsingException {
        return MerkleRoot.parse(seq.itemAt(i));
    }

    // ---------------------------------------------------------------------------------------------

    private static Address getAddress(RLP seq, int i) throws RLPParsingException {
        return Address.parse(seq.itemAt(i));
    }

    // ---------------------------------------------------------------------------------------------

    private static Natural getNatural(RLP seq, int i) throws RLPParsingException {
        return Natural.parse(seq.itemAt(i));
    }

    // ---------------------------------------------------------------------------------------------

    public static BloomFilter getBloomFilter (RLP seq, int i) throws RLPParsingException {
        return BloomFilter.parse(seq.itemAt(i));
    }

    // ---------------------------------------------------------------------------------------------
}
