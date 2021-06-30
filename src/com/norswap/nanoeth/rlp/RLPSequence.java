package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.Bytes;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * A sequence of other {@link RLPItem} that can be encoded to {@link RLP} format.
 */
public final class RLPSequence extends RLPItem {

    // ---------------------------------------------------------------------------------------------

    private final RLPItem[] items;

    // ---------------------------------------------------------------------------------------------

    private RLPSequence (@Retained RLPItem[] items) {
        this.items = items;
    }

    // ---------------------------------------------------------------------------------------------

    /** Create a sequence from the given items. */
    public static RLPSequence from (@Retained RLPItem... items) {
        return new RLPSequence(items);
    }

    // ---------------------------------------------------------------------------------------------

    /** Return the number of items in the sequence. */
    public int size() {
        return items.length;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public Bytes encode() {
        return RLPImplem.encode(this);
    }

    // ---------------------------------------------------------------------------------------------

    public Stream<RLPItem> stream() {
        return Arrays.stream(items);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RLPSequence that = (RLPSequence) o;
        return Arrays.equals(items, that.items);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(items);
    }

    @Override public String toString () {
        return Arrays.toString(items);
    }

    // ---------------------------------------------------------------------------------------------
}
