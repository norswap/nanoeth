package com.norswap.nanoeth.signature;

/**
 * Indicates that a signature is invalid: some of its parameters do not satisfy the signature
 * validity constraints.
 *
 * <p>Note that this exception is not thrown when verifying a signature, but only at {@link
 * Signature} instantiation time.
 */
public final class IllegalSignature extends Exception {
    public IllegalSignature (String message) {
        super(message);
    }
}
