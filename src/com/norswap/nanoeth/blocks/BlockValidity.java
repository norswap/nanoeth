package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.annotations.Nullable;

import java.util.Objects;

import static com.norswap.nanoeth.blocks.BlockValidityStatus.*;

/**
 * The block validity is a wrapper for {@link BlockValidityStatus} which includes additional
 * information if the block is invalid.
 */
public final class BlockValidity {

    // ---------------------------------------------------------------------------------------------

    /**
     * Wrapper for {@link BlockValidityStatus#VAL_VALID}.
     */
    public static final BlockValidity BLOCK_VALID = new BlockValidity(VAL_VALID, null);

    // ---------------------------------------------------------------------------------------------

    public final BlockValidityStatus status;

    // ---------------------------------------------------------------------------------------------

    /** The offending uncle, if the block is invalid because of a particular uncle. */
    public final @Nullable BlockHeader uncle;

    // ---------------------------------------------------------------------------------------------

    public static BlockValidity of (BlockValidityStatus status) {
        if (status == VAL_VALID)
            return BLOCK_VALID;
        return new BlockValidity(status, null);
    }

    public static BlockValidity of (BlockValidityStatus status, BlockHeader uncle) {
        switch (status) {
            case VAL_BAD_UNCLE:
            case VAL_UNCLE_TOO_OLD:
            case VAL_FUTURE_UNCLE:
            case VAL_UNCLE_ALREADY_INCLUDED:
            case VAL_UNCLE_IS_ANCESTOR:
            case VAL_UNRELATED_UNCLE:
                break;
            default:
                throw new AssertionError("wrong status for invalid uncle");
        }
        return new BlockValidity(status, uncle);
    }

    // ---------------------------------------------------------------------------------------------

    private BlockValidity (BlockValidityStatus status, @Nullable BlockHeader uncle) {
        this.status = status;
        this.uncle = uncle;
    }

    // ---------------------------------------------------------------------------------------------

    public boolean valid() {
        return status.valid();
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString() {
        return "BlockValidity (" + status + ")"
            + (uncle == null ? "" : "{ uncle=" + uncle + "}");
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof BlockValidity)) return false;
        BlockValidity that = (BlockValidity) o;
        return status == that.status && Objects.equals(uncle, that.uncle);
    }

    @Override public int hashCode() {
        return Objects.hash(status, uncle);
    }

    // ---------------------------------------------------------------------------------------------
}
