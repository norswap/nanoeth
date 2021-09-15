package com.norswap.nanoeth.rlp;

/**
 * Indicates an illegal access in an {@link RLP} object: trying to access a sequence when the object
 * represents a byte array or encoding, trying to access a byte array when the object represents
 * a sequence or encoding, or trying to access a byte or sub-sequence item that is out of bounds.
 */
public final class IllegalRLPAccess extends RuntimeException {
    public IllegalRLPAccess (String message) {
        super(message);
    }
}
