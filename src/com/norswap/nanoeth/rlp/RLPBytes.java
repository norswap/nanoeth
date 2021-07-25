package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.Bytes;
import java.util.Objects;

/**
 * A byte sequence that can be encoded to {@link RLP} format.
 */
public final class RLPBytes extends RLPItem {

    // ---------------------------------------------------------------------------------------------

    public final Bytes bytes;

    // ---------------------------------------------------------------------------------------------

    private RLPBytes (Bytes bytes) {
        assert bytes.frozen();
        this.bytes = bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /** Create a RLP byte sequence form the given bytes. */
    public static RLPBytes from (Bytes bytes) {
        return new RLPBytes(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    /** Create a RLP byte sequence form the given bytes. */
    public static RLPBytes from (@Retained byte... bytes) {
        return new RLPBytes(Bytes.from(bytes));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Bytes encode() {
        return RLP.encode(bytes);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof RLPBytes && bytes.equals(((RLPBytes) o).bytes);
    }

    @Override public int hashCode () {
        return Objects.hash(bytes);
    }

    @Override public String toString () {
        return bytes.toString();
    }

    // ---------------------------------------------------------------------------------------------
}
