package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.BloomFilter;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.data.Natural;
import java.util.Arrays;

// TODO timestamp & nonce should be <= 64 bits
public final class BlockHeader {

    // ---------------------------------------------------------------------------------------------

    /**
     * The {@link #hash} of the parent block.
     * <p>Yellowpaper notation: Hp
     */
    public final Hash parentHash;

    // ---------------------------------------------------------------------------------------------

    /**
     * Hash of the the uncles (aka "ommers") list of the block.
     * <p>Yellowpaper notation: Ho
     */
    public final Hash uncleHash;

    // ---------------------------------------------------------------------------------------------

    /**
     * Address that will receive the block reward (aka "beneficiary", aka "miner's address").
     * <p>Yellowpaper notation: Hc
     */
    public final Address coinbase;

    // ---------------------------------------------------------------------------------------------

    /**
     * The Merkle root of the state tree.
     * <p>Yellowpaper notation: Hr
     */
    public final MerkleRoot stateRoot;

    // ---------------------------------------------------------------------------------------------

    /**
     * The Merkle root of the transactions list of the block.
     * <p>Yellowpaper notation: Ht
     */
    public final MerkleRoot transactionsRoot;

    // ---------------------------------------------------------------------------------------------

    /**
     * The Merkle root of the receipts list of the block.
     * <p>Yellowpaper notation: He
     */
    public final MerkleRoot receiptsRoot;

    // ---------------------------------------------------------------------------------------------

    /**
     * The Bloom filter composed from indexable information (logger address and log topics)
     * contained in each log entry from the receipt of each transaction in the transactions list.
     * <p>Yellowpaper notation: Hb
     */
    public final BloomFilter logsBloom;

    // ---------------------------------------------------------------------------------------------

    // TODO timestamp, is that so?
    /**
     * The difficult level of the block, calculated from the previous' block difficulty level &
     * the {@link #timestamp}.
     */
    public final Natural difficulty;

    // ---------------------------------------------------------------------------------------------

    /**
     * The number of ancestor blocks (aka "height"). The genesis block has number 0.
     * <p>Yellowpaper notation: Hi
     */
    public final Natural number;

    // ---------------------------------------------------------------------------------------------

    // TODO how is this set?
    /**
     * Current maximum amount of gas usable per block.
     * <p>Yellowpaper notation: Hl
     */
    public final Natural gasLimit;

    // ---------------------------------------------------------------------------------------------

    /**
     * The amount of gas used in this block.
     * <p>Yellowpaper notation: Hg</p>
     */
    public final Natural gasUsed;

    // ---------------------------------------------------------------------------------------------

    /**
     * A value equal to the reasonable output of Unix’s time() at this block’s inception.
     * <p>Yellowpaper notation: Hs
     */
    public final Natural timestamp;

    // ---------------------------------------------------------------------------------------------

    /**
     * An arbitrary byte array containing data relevant to this block.
     * This must be 32 bytes or fewer.
     * <p>Yellowpaper notaion: Hx
     */
    public final byte[] extraData;

    // ---------------------------------------------------------------------------------------------

    /**
     * A hash which, combined with the {@link #nonce}, proves that a sufficient amount of
     * computation has been carried out on this block.
     * <p>Yellowpaper notation: Hm
     */
    public final Hash mixHash;

    // ---------------------------------------------------------------------------------------------

    /**
     * A 64-bit scalar value which, combined with the {@link #mixHash}, proves that a sufficient
     * amount of computation has been carried out on this block.
     *
     * <p>Miners iterate the nonce value until they can find a {@link #mixHash} that satisfies
     * the {@link #difficulty} requirement.
     *
     * <p>Yellowpaper notation: Hn
     */
    public final Natural nonce;

    // ---------------------------------------------------------------------------------------------

    /**
     * Hash of all the other data in the header. This is not serialized in the block (but will
     * be serialized in the children as {@link #parentHash}).
     */
    public final Hash hash;

    // ---------------------------------------------------------------------------------------------

    public BlockHeader(
            Hash parentHash,
            Hash uncleHash,
            Address coinbase,
            MerkleRoot stateRoot,
            MerkleRoot transactionsRoot,
            MerkleRoot receiptsRoot,
            BloomFilter logsBloom,
            Natural difficulty,
            Natural number,
            Natural gasLimit,
            Natural gasUsed,
            Natural timestamp,
            byte[] extraData,
            Hash mixHash,
            Natural nonce,
            Hash hash) {

        this.parentHash = parentHash;
        this.uncleHash = uncleHash;
        this.coinbase = coinbase;
        this.stateRoot = stateRoot;
        this.transactionsRoot = transactionsRoot;
        this.receiptsRoot = receiptsRoot;
        this.logsBloom = logsBloom;
        this.difficulty = difficulty;
        this.number = number;
        this.gasLimit = gasLimit;
        this.gasUsed = gasUsed;
        this.timestamp = timestamp;
        this.extraData = extraData;
        this.mixHash = mixHash;
        this.nonce = nonce;
        this.hash = hash;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return "BlockHeader{" +
            "parentHash=" + parentHash +
            ", uncleHash=" + uncleHash +
            ", coinbase=" + coinbase +
            ", stateRoot=" + stateRoot +
            ", transactionsRoot=" + transactionsRoot +
            ", receiptsRoot=" + receiptsRoot +
            ", logsBloom=" + logsBloom +
            ", difficulty=" + difficulty +
            ", number=" + number +
            ", gasLimit=" + gasLimit +
            ", gasUsed=" + gasUsed +
            ", timestamp=" + timestamp +
            ", extraData=" + Arrays.toString(extraData) +
            ", mixHash=" + mixHash +
            ", nonce=" + nonce +
            ", hash=" + hash +
            '}';
    }

    // ---------------------------------------------------------------------------------------------
}
