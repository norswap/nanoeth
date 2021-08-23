package com.norswap.nanoeth.blocks;

/**
 * TODO
 */
public enum BlockValidity {
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
    VAL_INVALID_POW;
}
