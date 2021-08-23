package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.transactions.Transaction;
import com.norswap.nanoeth.transactions.TransactionEnvelopeType;
import java.util.Arrays;
import java.util.Objects;

/**
 * Where even to begin? The block in blockchain, the one that holds transactions.
 */
public final class Block {

    // ---------------------------------------------------------------------------------------------

    /**
     * Maximum number of uncle that can be included in the block.
     * TODO: specified where?
     */
    public static final int MAX_UNCLES = 2;

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

    public Block (BlockHeader header, Transaction[] transactions, BlockHeader[] uncles) {
        this.header = header;
        this.transactions = transactions;
        this.uncles = uncles;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses a RLP sequence into a block.
     *
     * @throws IllegalBlockFormatException
     * if the RLP sequence does not properly parse to a block
     */
    public static Block from (RLP rlp) throws IllegalBlockFormatException {
        return BlockParser.parseBlock(rlp);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Validates the block.
     *
     * @return {@link BlockValidity#VAL_VALID} if the block is valid, or a {@link BlockValidity}
     *      value that indicates the reason for the failure.
     */
    public BlockValidity validate() {
        // TODO hash uncles & compare to uncleHash from header
        // TODO build transactions Merkle tree and check transactionRoot from header
        // TODO run transactions
        //  & check stateRoot from header
        //  & check receiptsRoot from header
        //  & check logsBloom from header

        // The header validation will handle the case where we don't know the parent hash.
        return header.validate(Blocks.DB.getHeader(header.parentHash));
    }

    // ---------------------------------------------------------------------------------------------

    // TODO speculative, not an execution/consensus concern, only a JSON-RPC concern
    /**
     * Returns the RLP representation of this block, whose binary encoding is used to transmit the
     * block over the network.
     */
    public RLP rlp() {
        var txs = Arrays.stream(transactions).map(tx ->
            tx.format.type == TransactionEnvelopeType.ENVELOPE_TYPE_NONE
                ? tx.rlp()
                : RLP.sequence(tx.format.type, tx.rlp())).toArray();
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
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return header.equals(block.header) && Arrays.equals(transactions,
                block.transactions) && Arrays.equals(uncles, block.uncles);
    }

    @Override
    public int hashCode () {
        int result = Objects.hash(header);
        result = 31 * result + Arrays.hashCode(transactions);
        result = 31 * result + Arrays.hashCode(uncles);
        return result;
    }


    // ---------------------------------------------------------------------------------------------
}
