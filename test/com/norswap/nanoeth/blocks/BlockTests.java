package com.norswap.nanoeth.blocks;

import com.norswap.nanoeth.Config;
import norswap.utils.Vanilla;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static com.norswap.nanoeth.Context.CONTEXT;
import static norswap.utils.Util.cast;
import static org.testng.Assert.assertEquals;

public final class BlockTests {

    // ---------------------------------------------------------------------------------------------

    @DataProvider public static Object[][] blocks () {
        return OfficialBlockData.TEST_CASES.stream()
            .map(t -> new Object[] { t })
            .toArray(Object[][]::new);
    }

    // ---------------------------------------------------------------------------------------------

    @Test(dataProvider = "blocks")
    public void testBlocks (BlockTestCase testCase)
            throws IllegalBlockFormatException {

        Config.VALIDATE_POW = testCase.validatePoW;
        CONTEXT.blockHeight = testCase.blockHeight;
        testValidBlock(testCase);
        CONTEXT.reset();
    }

    // ---------------------------------------------------------------------------------------------

    private void testValidBlock (BlockTestCase testCase)
            throws IllegalBlockFormatException {

        assertEquals(testCase.genesis.rlp().toHexString(), testCase.genesisRLP);

        // TODO compress memory requirements
        List<Block> blocks = cast(Vanilla.concat(testCase.genesis, testCase.blocks));
        for (var block: blocks) {
            assertEquals(Block.from(block.rlp()), block);
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

        testBlockValidity(testCase);
    }

    // ---------------------------------------------------------------------------------------------

    private void testBlockValidity (BlockTestCase testCase) {
        // Some test case have irregular genesis difficulties.
        Config.GENESIS_DIFFICULTY = testCase.genesis.header.difficulty;
        Config.VALIDATE_POW = testCase.validatePoW;

        List<Block> blocks = cast(Vanilla.concat(testCase.genesis, testCase.blocks));
        for (var block: blocks) {
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
