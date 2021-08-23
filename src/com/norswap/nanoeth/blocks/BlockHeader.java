package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.Config;
import com.norswap.nanoeth.receipts.BloomFilter;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.MerkleRoot;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.rlp.RLP;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.Hashing;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Objects;

import static com.norswap.nanoeth.blocks.BlockValidity.*;

public final class BlockHeader {

    // ---------------------------------------------------------------------------------------------

    /**
     * Hash of the empty RLP sequence, which can be compared against the uncle hash to see if the
     * block has any uncles.
     */
    public static final Hash EMPTY_SEQ_HASH = Hashing.keccak(RLP.sequence(new Object[0]).encode());

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
     * The difficult level of the block, calculated from the previous' block difficulty level and
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

    // TODO whitepaper: N256? serialized as such???

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
     *
     * <p>Note that unlike other scalar values, by like addresses, hashes, ... the nonce gets
     * serialized in full, even if it has leading zeroes.
     */
    public final long nonce;

    // ---------------------------------------------------------------------------------------------

    /**
     * For hash caching, access via {@link #hash()}.
     */
    private Hash hash;

    // ---------------------------------------------------------------------------------------------

    /** Constructs a header from header data, and computes the header hash from this data. */
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
            long nonce) {

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
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Hash of all the other data in the header. This is not serialized in the block (but will
     * be serialized in the children as {@link #parentHash}). This is computed lazily and cached.
     */
    public Hash hash() {
        return hash != null
            ? hash
            : (hash = Hashing.keccak(rlp().encode()));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses a RLP sequence into a block header.
     *
     * @throws IllegalBlockFormatException
     * if the RLP sequence does not properly parse to a block header
     */
    public static BlockHeader from (RLP rlp) throws IllegalBlockFormatException {
        return BlockParser.parseHeader(rlp);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Validates the block header against its parent. The parent is expected to have been
     * retrieve from this header's {@link #parentHash}.
     *
     * @return {@link BlockValidity#VAL_VALID} if the header is valid, or a {@link BlockValidity}
     *      value that indicates the reason for the failure.
     *
     * @see Block#validate() for full block validation, including running the transactions.
     */
    public BlockValidity validate (BlockHeader parent) {

        // NOTE(norswap): In a real client, a number of these things should already be checked
        //  upstream (in the networking layer) to avoid DoS via huge blocks. Nevertheless it's good
        //  to check them here anyway for clarity & in case there are secondary ingress points
        //  (e.g. tests).

        if (parent == null) {
            if (number.equals(BigInteger.ZERO))
                return VAL_VALID; // TODO validate genesis
            return VAL_UNKNOWN_PARENT;
        }

        // We assume that this is correct, as we use the hash in the header to retrieve the parent.
        assert parentHash.equals(parent.hash());

        if (timestamp.compareTo(parent.timestamp) <= 0)
            return VAL_OUTDATED_TIMESTAMP;

        if (!number.equals(parent.number.add(1)))
            return VAL_BAD_NUMBER;

        if (extraData.length > 32)
            return VAL_EXTRA_DATA_TOO_LONG;

        var gasLimitIncrement = parent.gasLimit.divide(1024);

        if (gasLimit.compareTo(parent.gasLimit.add(gasLimitIncrement)) > 0)
            return VAL_GAS_LIMIT_TOO_HIGH;

        if (gasLimit.compareTo(parent.gasLimit.subtract(gasLimitIncrement)) < 0)
            return VAL_GAS_LIMIT_TOO_LOW;

        if (gasLimit.lower(5000)) // minimum gas limit
            return VAL_GAS_LIMIT_TOO_LOW;

        if (gasUsed.compareTo(gasLimit) > 0)
            return VAL_GAS_USED_TOO_HIGH;

        if (Config.VALIDATE_POW) {
            if (!difficulty.equals(Difficulty.computeDifficulty(timestamp, parent)))
                return VAL_BAD_DIFFICULTY;

            var maxNonce = BigInteger.TWO.pow(256).divide(difficulty);

            if (new Natural(nonce).compareTo(maxNonce) > 0)
                return VAL_NONCE_TOO_HIGH;

            if (Config.VALIDATE_POW && !ProofOfWork.verifyPoW(this))
                return VAL_INVALID_POW;
        }

        return VAL_VALID;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the RLP representation of this block header, which is stored in the block (itself
     * RLP-encoded) when circulated over the network. Uncle block headers are similarly encoded.
     */
    public RLP rlp() {
        return RLP.sequence(
            parentHash, uncleHash, coinbase, stateRoot, transactionsRoot, receiptsRoot, logsBloom,
            difficulty, number, gasLimit, gasUsed, timestamp, extraData, mixHash, nonce);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockHeader)) return false;
        BlockHeader that = (BlockHeader) o;
        return nonce == that.nonce
            && parentHash.equals(that.parentHash)
            && uncleHash.equals(that.uncleHash)
            && coinbase.equals(that.coinbase)
            && stateRoot.equals(that.stateRoot)
            && transactionsRoot.equals(that.transactionsRoot)
            && receiptsRoot.equals(that.receiptsRoot)
            && logsBloom.equals(that.logsBloom)
            && difficulty.equals(that.difficulty)
            && number.equals(that.number)
            && gasLimit.equals(that.gasLimit)
            && gasUsed.equals(that.gasUsed)
            && timestamp.equals(that.timestamp)
            && Arrays.equals(extraData,that.extraData)
            && mixHash.equals(that.mixHash);
    }

    @Override public int hashCode () {
        int result = Objects.hash(parentHash, uncleHash, coinbase, stateRoot, transactionsRoot,
            receiptsRoot, logsBloom, difficulty, number, gasLimit, gasUsed, timestamp, mixHash,
            nonce);
        return 31 * result + Arrays.hashCode(extraData);
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
            ", hash=" + hash() +
            '}';
    }

    // ---------------------------------------------------------------------------------------------
}
