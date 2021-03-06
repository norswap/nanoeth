package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.Config;
import com.norswap.nanoeth.rlp.RLPParsingException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.norswap.nanoeth.Context.CONTEXT;
import static org.testng.Assert.assertEquals;

public final class BlockTests {

    // ---------------------------------------------------------------------------------------------

    @DataProvider public static Object[][] blocks () {
        return SharedBlockData.TEST_CASES.stream()
            .map(t -> new Object[] { t })
            .toArray(Object[][]::new);
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "blocks")
    public void testBlocks (BlockTestCase testCase) throws RLPParsingException {

        Config.VALIDATE_POW = testCase.validatePoW;
        CONTEXT.blockHeight = testCase.blockHeight;
        testValidBlocks(testCase);
        CONTEXT.reset();
    }

    // ---------------------------------------------------------------------------------------------

    private void testValidBlocks (BlockTestCase testCase) throws RLPParsingException {

        assertEquals(testCase.genesis.rlpHexString(), testCase.genesisRLP);
        Config.GENESIS = testCase.genesis;

        for (var block: testCase.blocks) {
            assertEquals(Block.from(block.rlpLayout()), block);
            Blocks.DB.register(block);
            if (testCase.validatePoW) {
                var computedDifficulty = Difficulty.computeDifficulty(
                        block.header.timestamp,
                        Blocks.DB.getHeader(block.header.parentHash));

                assertEquals(block.header.difficulty, computedDifficulty);
            }
            assertEquals(block.validate(), BlockValidity.BLOCK_VALID);
        }

        Blocks.DB.clear();
    }

    // ---------------------------------------------------------------------------------------------
}
