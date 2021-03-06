package com.norswap.nanoeth;

import com.norswap.nanoeth.versions.EthereumVersion;

/**
 * A holder for values that may change during execution, but we don't want to be constantly
 * passing up and down the call stack.
 */
public final class Context {
    private Context() {}

    // ---------------------------------------------------------------------------------------------

    /**
     * A holder for values that may change during execution, but we don't want to be constantly
     * passing up and down the call stack.
     */
    public static final Context CONTEXT = new Context();

    // ---------------------------------------------------------------------------------------------

    /**
     * The current block height. Some operations whose behaviour was changed in a {@link
     * EthereumVersion hard fork} depend on this value. In particular:
     * <ul>
     * <li>EIP-2 forces signature s values to be {@code <} secp256k1n/2</li>
     * </ul>
     */
    public long blockHeight = EthereumVersion.LONDON.startBlock;

    // ---------------------------------------------------------------------------------------------

    /** Resets the context to default values. */
    public void reset () {
        blockHeight = EthereumVersion.LONDON.startBlock;
    }

    // ---------------------------------------------------------------------------------------------
}
