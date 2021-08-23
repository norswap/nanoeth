package com.norswap.nanoeth.data;

import com.norswap.nanoeth.annotations.Retained;
import com.norswap.nanoeth.annotations.Wrapper;
import com.norswap.nanoeth.utils.Assert;
import com.norswap.nanoeth.utils.ByteUtils;
import java.util.Arrays;

/**
 * Represents a 256-bit (32 bytes) Ethereum storage key within an account's storage tree.
 */
@Wrapper
public final class StorageKey {
    // ---------------------------------------------------------------------------------------------

    /** The 32-bytes big-endian representation of the storage key. */
    public final byte[] bytes;

    // ---------------------------------------------------------------------------------------------

    public StorageKey (@Retained byte[] bytes) {
        Assert.that(bytes.length == 32, "storage key is not 32 bytes long");
        this.bytes = bytes;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a hash from a hex string (e.g. 0x123).
     *
     * <p>If the post-0x part of the hex string is not 64 characters long, the key will be padded
     * with zeroes at the start so that it is 32 bytes long.
     */
    public StorageKey (String hexString) {
        this(ByteUtils.hexStringToBytes(hexString, 32));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        return this == o || o instanceof StorageKey && Arrays.equals(bytes, ((StorageKey) o).bytes);
    }

    @Override public int hashCode () {
        return Arrays.hashCode(bytes);
    }

    @Override public String toString() {
        return ByteUtils.toCompressedHexString(bytes);
    }

    // ---------------------------------------------------------------------------------------------
}
