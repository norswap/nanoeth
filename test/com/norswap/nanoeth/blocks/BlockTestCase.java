package com.norswap.nanoeth.blocks;

public final class BlockTestCase {

    // ---------------------------------------------------------------------------------------------

    public final String name;

    public final long blockHeight;

    public final String genesisRLP;

    public final Block genesis;

    public final Block[] blocks;

    public final boolean validatePoW;

    // ---------------------------------------------------------------------------------------------

    public BlockTestCase (
            String name, long blockHeight, String genesisRLP, Block genesis, Block[] blocks,
            boolean validatePoW) {
        this.name = name;
        this.blockHeight = blockHeight;
        this.genesisRLP = genesisRLP;
        this.genesis = genesis;
        this.blocks = blocks;
        this.validatePoW = validatePoW;
    }

    // ---------------------------------------------------------------------------------------------

    @Override public String toString () {
        return name;
    }

    // ---------------------------------------------------------------------------------------------
}
