package com.norswap.nanoeth.crypto;

import org.testng.annotations.Test;

import static com.norswap.nanoeth.crypto.Bandersnatch.BANDERSNATCH;
import static org.testng.Assert.*;

public final class BandersnatchTests {

    @Test public void testBandersnatch() {
        // verify that n is indeed the size of the cyclic subgroup generate by G
        assertTrue(BANDERSNATCH.G().multiply(BANDERSNATCH.n()).isInfinity());

        // TODO remove the thing that made this success (in add and negate), and try the previous tests
        //   see if re-adding the fix changes anything
    }
}
