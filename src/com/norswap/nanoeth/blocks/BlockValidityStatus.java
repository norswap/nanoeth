package com.norswap.nanoeth.blocks;

/**
 * The validity status of a block, which is either valid ({@link #VAL_VALID}), or another status
 * indicating why the block is invalid.
 *
 * @see BlockValidity
 */
public enum BlockValidityStatus {

    // ---------------------------------------------------------------------------------------------

    /** The block is valid. */
    VAL_VALID,
    /** We don't know of a block with the given parent hash. */
    VAL_UNKNOWN_PARENT,
    /** The timestamp is older than the parent's timestamp. */
    VAL_OUTDATED_TIMESTAMP,
    /** The number is not the parent number + 1. */
    VAL_BAD_NUMBER,
    /** The extra data section is longer than 32 bytes. */
    VAL_EXTRA_DATA_TOO_LONG,
    /** The difficulty does not match the canonical difficulty calculation. */
    VAL_BAD_DIFFICULTY,
    /** The gas limit is too high compared to the parent. */
    VAL_GAS_LIMIT_TOO_HIGH,
    /** The gas limit is too low compared to the parent, or under the minimum. */
    VAL_GAS_LIMIT_TOO_LOW,
    /** The gas usage is higher than the gas limit. */
    VAL_GAS_USED_TOO_HIGH,
    /** The nonce does not match the difficulty. */
    VAL_NONCE_TOO_HIGH,
    /** The nonce and the mix hash are not consistent with regard to the proof of work calculation. */
    VAL_INVALID_POW,
    /** Too many uncles were included. */
    VAL_TOO_MANY_UNCLES,
    /** One of the uncle header is invalid. */
    VAL_BAD_UNCLE,
    /** One of the uncle headers' number is too old (> 6 back). */
    VAL_UNCLE_TOO_OLD,
    /** The uncle's number is the same or greater than the block's number. */
    VAL_FUTURE_UNCLE,
    /** The uncle hash does not match the uncle headers. */
    VAL_BAD_UNCLE_HASH,
    /** An uncle has already appeared in an ancestor. */
    VAL_UNCLE_ALREADY_INCLUDED,
    /** One or multiple uncles appear multiple time in the block. */
    VAL_DUPLICATE_UNCLE,
    /** An uncle has already appeared *as* an ancestor. */
    VAL_UNCLE_IS_ANCESTOR,
    /** An included uncle isn't really an uncle (the sibling of an ancestor of degree <= 6. */
    VAL_UNRELATED_UNCLE;

    // ---------------------------------------------------------------------------------------------

    public boolean valid() {
        return this == VAL_VALID;
    }

    // ---------------------------------------------------------------------------------------------
}
