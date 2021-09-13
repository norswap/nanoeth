package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.rlp.RLPParsingException;
import com.norswap.nanoeth.transactions.Transaction;
import com.norswap.nanoeth.trees.patricia.PatriciaTree;
import com.norswap.nanoeth.utils.ByteUtils;
import com.norswap.nanoeth.utils.Utils;
import java.util.Arrays;
import java.util.Objects;

import static com.norswap.nanoeth.blocks.BlockValidity.BLOCK_VALID;
import static com.norswap.nanoeth.blocks.BlockValidityStatus.*;

/**
 * Where even to begin? The block in blockchain, the one that holds transactions.
 */
public final class Block {

    // ---------------------------------------------------------------------------------------------

    /**
     * Maximum number of uncle that can be included in the block, which is 2, as per section 11.1
     * of the yellowpaper.
     */
    public static final int MAX_UNCLES = 2;

    // ---------------------------------------------------------------------------------------------

    /**
     * Maximum uncle degree (e.g. degree 1 = sibling of parent, degree 2 = sibling of grandparent).
     */
    public static final int MAX_UNCLE_DEGREE = 6;

    // ---------------------------------------------------------------------------------------------

    public final BlockHeader header;

    // ---------------------------------------------------------------------------------------------

    public final Transaction[] transactions;

    // ---------------------------------------------------------------------------------------------

    /**
     * Uncle (aka ommer) blocks are children of ancestors of this block that are not themselves
     * ancestor of these block, and have not been included as uncles in ancestors.
     *
     * <p>When included in a block, the coinbase of the uncle is credited 7/8 of the normal mining
     * reward. The miner also receives 1/32 of the mining reward per included uncle. Uncles
     * disencentivize chain reorganizations, by making it profitable to publish uncles instead of
     * continuing to mine on one's own fork. (However, unlike the in the GHOST protocol that
     * inspired the use of uncles, Ethereum does not count unclestowards the weight of the chain in
     * the fork-choice rule.)
     */
    public final BlockHeader[] uncles;

    // ---------------------------------------------------------------------------------------------

    public Block (
            BlockHeader header,
            @Retained Transaction[] transactions,
            @Retained BlockHeader[] uncles) {
        this.header = header;
        this.transactions = transactions;
        this.uncles = uncles;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses a RLP sequence into a block.
     *
     * @throws RLPParsingException
     * if the RLP sequence does not properly parse to a block
     */
    public static Block from (RLP rlp) throws RLPParsingException {
        return BlockParser.parseBlock(rlp);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Validates the block, returning a {@link BlockValidity} object to indicate if the block
     * valid or invalid (and why).
     */
    public BlockValidity validate() {

        var uncleValidity = validateUncles();
        if (!uncleValidity.valid())
            return uncleValidity;

        var headerValidity = BlockValidity.of(header.validate());
        if (!headerValidity.valid()) return headerValidity;

        var txTree = new PatriciaTree();
        for (int i = 0; i < transactions.length; i++) {
            byte[] key   = RLP.bytes(ByteUtils.bytes(i)).encode();
            byte[] value = transactions[i].binary();
            txTree = txTree.add(key, value);
        }

        if (!txTree.merkleRoot().equals(header.transactionsRoot))
            return BlockValidity.of(VAL_BAD_TX_ROOT);

        return BLOCK_VALID;

        // TODO
        //   - run transactions
        //   & check stateRoot from header
        //   & check receiptsRoot from header
        //   & check logsBloom from header
    }

    // ---------------------------------------------------------------------------------------------

    /** Validate uncles, as per section 11.1 of the yellowpaper. */
    private BlockValidity validateUncles() {

        if (uncles.length > MAX_UNCLES)
            return BlockValidity.of(VAL_TOO_MANY_UNCLES);

        for (var uncle: uncles) {
            if (uncle.validate() != VAL_VALID)
                return BlockValidity.of(VAL_BAD_UNCLE, uncle);
            if (uncle.number.compareTo(header.number) >= 0)
                return BlockValidity.of(VAL_UNCLE_TOO_OLD, uncle);
            if (uncle.number.compareTo(header.number.subtract(new Natural(MAX_UNCLE_DEGREE))) < 0)
                return BlockValidity.of(VAL_FUTURE_UNCLE, uncle);

            // NOTE: This surprisingly doesn't seem to specified in the yellowpaper.
            var lineageValidity = validateUncleLineage(uncle);
            if (!lineageValidity.valid())
                return lineageValidity;
        }

        // NOTE: This surprisingly doesn't seem to specified in the yellowpaper.
        if (uncles.length > 1 && !Utils.allDistinct(uncles))
            return BlockValidity.of(VAL_DUPLICATE_UNCLE);

        if (!RLP.sequence((Object[]) uncles).hash().equals(header.uncleHash))
            return BlockValidity.of(VAL_BAD_UNCLE_HASH);

        return BLOCK_VALID;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Checks that (1) {@code uncle} really is an uncle (the sibling of an ancestor of degree <=
     * {@link #MAX_UNCLE_DEGREE}) and (2) that the uncle hasn't been previously included as an uncle
     * or main chain block.
     */
    private BlockValidity validateUncleLineage (BlockHeader uncle) {
        boolean isUncle = false;
        var uncleParentHeader = Blocks.DB.getHeader(uncle.parentHash);
        var current = this;

        // We need to go to the (max_degree + 1)th ancestor to check if it's not a (max_degree)th
        // generation uncle. The logic stays valid.
        for (int i = 1; i <= MAX_UNCLE_DEGREE + 1; ++i) {
            current = Blocks.DB.get(current.header.parentHash);

            if (current == null) { // genesis
                isUncle |= uncleParentHeader == null;
                break;
            }

            if (current.header.number.equals(uncle.number)
                    && current.header.equals(uncle))
                return BlockValidity.of(VAL_UNCLE_IS_ANCESTOR, uncle);

            if (uncleParentHeader != null && current.header.number.equals(uncleParentHeader.number))
                isUncle |= current.header.equals(uncleParentHeader);

            if (Arrays.asList(current.uncles).contains(uncle))
                return BlockValidity.of(VAL_UNCLE_ALREADY_INCLUDED, uncle); 
        }

        return isUncle ? BLOCK_VALID : BlockValidity.of(VAL_UNRELATED_UNCLE, uncle);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the RLP representation of this block, whose binary encoding is used to transmit the
     * block over the network.
     */
    public RLP rlp() {
        var txs = Arrays.stream(transactions).map(Transaction::rlp).toArray();
        return RLP.sequence(header.rlp(), txs, uncles);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return "Block{" +
            "header=" + header +
            ", transactions=" + Arrays.toString(transactions) +
            ", uncles=" + Arrays.toString(uncles) +
            '}';
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof Block)) return false;
        Block block = (Block) o;
        return header.equals(block.header) && Arrays.equals(transactions,
            block.transactions) && Arrays.equals(uncles, block.uncles);
    }

    @Override public int hashCode () {
        int result = Objects.hash(header);
        result = 31 * result + Arrays.hashCode(transactions);
        result = 31 * result + Arrays.hashCode(uncles);
        return result;
    }

    // ---------------------------------------------------------------------------------------------
}
