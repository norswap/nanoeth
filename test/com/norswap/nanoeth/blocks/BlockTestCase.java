package com.norswap.nanoeth.blocks;

import norswap.utils.Vanilla;

public final class BlockTestCase {

    // ---------------------------------------------------------------------------------------------

    public final String name;

    public final long blockHeight;

    public final String genesisRLP;

    public final Block genesis;

    public final Block[] blocks;

    public final boolean validatePoW;

    // ---------------------------------------------------------------------------------------------

    @SuppressWarnings("SuspiciousToArrayCall")
    public BlockTestCase (
            String name, long blockHeight, String genesisRLP, Block genesis, Block[] blocks,
            boolean validatePoW) {
        this.name = name;
        this.blockHeight = blockHeight;
        this.genesisRLP = genesisRLP;
        this.genesis = genesis;
        this.blocks = Vanilla.concat(genesis, blocks).toArray(Block[]::new);
        this.validatePoW = validatePoW;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return name;
    }

    // ---------------------------------------------------------------------------------------------
}
