package com.norswap.nanoeth.rlp;

/**
 * Indicates an illegal access in an {@link RLP} object: either trying to access a sequence when
 * the object represents a byte (or vice-versa), or trying to access a byte or sub-sequence item
 * that is out of bounds.
 */
public final class IllegalRLPAccess extends RuntimeException {
    public IllegalRLPAccess (String message) {
        super(message);
    }
}
