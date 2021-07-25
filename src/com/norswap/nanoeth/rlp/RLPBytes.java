package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.annotations.Retained;
import java.util.Arrays;
import java.util.Objects;

/**
 * A byte sequence that can be encoded to {@link RLP} format.
 */
public final class RLPBytes extends RLPItem {

    // ---------------------------------------------------------------------------------------------

    public final byte[] bytes;

    // ---------------------------------------------------------------------------------------------

    /** Create a RLP byte sequence form the given bytes. */
    public RLPBytes (@Retained byte... bytes) {
        this.bytes = bytes;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] encode() {
        return RLP.encode(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof RLPBytes && Arrays.equals(bytes, ((RLPBytes) o).bytes);
    }

    @Override public int hashCode () {
        Object bytes = this.bytes; // avoid spurious variadic arg warnings
        return Objects.hash(bytes);
    }

    @Override public String toString () {
        return Arrays.toString(bytes);
    }

    // ---------------------------------------------------------------------------------------------
}
