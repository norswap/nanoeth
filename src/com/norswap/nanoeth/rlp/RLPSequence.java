package com.norswap.nanoeth.rlp;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Hash;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.data.StorageKey;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.ArrayList;
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

    /**
     * Creates a sequence from the given items, automatically translating them according to their
     * types. Supported types are:
     * <ul>
 *     <li>{@code byte[]}</li>
     * <li>{@link RLPItem}</li>
     * <li>{@link Integer} (can pass an {@code int}</li>
     * <li>{@link Natural}</li>
     * <li>{@link Address}</li>
     * <li>{@link Hash}</li>
     * <li>{@link StorageKey}</li>
     * </ul>
     */
    public static RLPSequence from (Object... items) {
        var converted = new ArrayList<RLPItem>();
        for (var item: items) {
            if (item instanceof RLPItem)
                converted.add((RLPItem) item);
            else if (item instanceof Integer)
                converted.add(new RLPBytes(ByteUtils.bytes((Integer) item)));
            else if (item instanceof Natural)
                converted.add(new RLPBytes(ByteUtils.bytesWithoutSign((Natural) item)));
            else if (item instanceof byte[])
                converted.add(new RLPBytes((byte[]) item));
            else if (item instanceof Address)
                converted.add(new RLPBytes(((Address) item).bytes));
            else if (item instanceof Hash)
                converted.add(new RLPBytes(((Hash) item).bytes));
            else if (item instanceof StorageKey)
                converted.add(new RLPBytes(((StorageKey) item).bytes));
            else throw new IllegalArgumentException(
                "unhandled conversion from type: " + item.getClass());
        }
        return from(converted.toArray(new RLPItem[items.length]));
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

    public RLPItem get (int i) {
        return items[i];
    }

    // ---------------------------------------------------------------------------------------------

    @Override public byte[] encode() {
        return RLP.encode(this);
    }

    // ---------------------------------------------------------------------------------------------

    public Stream<RLPItem> stream() {
        return Arrays.stream(items);
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof RLPSequence && Arrays.equals(items, ((RLPSequence) o).items);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(items);
    }

    @Override public String toString () {
        return Arrays.toString(items);
    }

    // ---------------------------------------------------------------------------------------------
}
