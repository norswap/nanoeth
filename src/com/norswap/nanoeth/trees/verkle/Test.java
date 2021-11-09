package com.norswap.nanoeth.trees.verkle;

import com.norswap.nanoeth.crypto.Bandersnatch;
import com.norswap.nanoeth.crypto.Crypto;
import com.norswap.nanoeth.crypto.Curve;
import com.norswap.nanoeth.utils.Randomness;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

public class Test {
    // ---------------------------------------------------------------------------------------------

    /** The curve used for proofs. */
//    public static final Curve CURVE = Curve.SECP256K1;
    public static final Curve CURVE = Bandersnatch.BANDERSNATCH;

    // ---------------------------------------------------------------------------------------------

    /** A set of nothing-up-my-sleeve points used to construct vector commitments. */
    private static final ECPoint[] PEDERSEN_BASIS =
        Crypto.nothingUpMySleevePoints(CURVE, 513);

    // ---------------------------------------------------------------------------------------------

    // TODO: how do I open a commitment to a specific position?

    public static void main (String[] args) {
        // TODO uncomment
        sanityChecks();
        testNonHidingVectorsProof();
        testVectorsProof();
        testInnerProof();
    }

    // ---------------------------------------------------------------------------------------------

    private static void sanityChecks() {
        int numVectors = 4;
        int vectorSize = 2;
        var vectors = new BigInteger[numVectors][];
        for (int i = 0; i < vectors.length; i++)
            vectors[i] = Randomness.randomIntegers(vectorSize);

        var v0 = vectors[0];

        var manualCommitment  = PEDERSEN_BASIS[0].multiply(v0[0]).add(PEDERSEN_BASIS[1].multiply(v0[1]));
        var reverseCommitment = PEDERSEN_BASIS[1].multiply(v0[1]).add(PEDERSEN_BASIS[0].multiply(v0[0]));
        assert manualCommitment.equals(reverseCommitment);
        assert manualCommitment.equals(Crypto.pedersenCommitment(v0, PEDERSEN_BASIS));
    }

    // ---------------------------------------------------------------------------------------------

    private static void testVectorsProof() {
        int numVectors = 4;
        int vectorSize = 100;
        var randoms = Randomness.randomIntegers(numVectors);
        var vectors = new BigInteger[numVectors][];
        for (int i = 0; i < vectors.length; i++)
            vectors[i] = Randomness.randomIntegers(vectorSize);
        var commitments = new ECPoint[numVectors];
        for (int i = 0; i < commitments.length; i++)
            commitments[i] = Crypto.pedersenCommitment(randoms[i], vectors[i], PEDERSEN_BASIS);
        var proof = new VectorsProof(PEDERSEN_BASIS, vectorSize, randoms, vectors);
        System.out.println(proof.verify(commitments));
    }

    // ---------------------------------------------------------------------------------------------

    private static void testNonHidingVectorsProof() {
        int numVectors = 4;
        int vectorSize = 100;
        var vectors = new BigInteger[numVectors][];
        for (int i = 0; i < vectors.length; i++)
            vectors[i] = Randomness.randomIntegers(vectorSize);
        var commitments = new ECPoint[numVectors];
        for (int i = 0; i < commitments.length; i++) {
            commitments[i] = Crypto.pedersenCommitment(vectors[i], PEDERSEN_BASIS);
            System.out.println(commitments[i]);
        }
        var proof = new NonHidingVectorsProof(PEDERSEN_BASIS, vectorSize, vectors);
        System.out.println(proof.verify(commitments));
    }

    // ---------------------------------------------------------------------------------------------

    private static void testInnerProof() {
        int vectorSize = 256;
        var vector1 = Randomness.randomIntegers(vectorSize);
        var vector2 = Randomness.randomIntegers(vectorSize);
        var proof = new InnerProductProof(CURVE, PEDERSEN_BASIS, vector1, vector2);
        System.out.println(proof.verify());
    }

    // ---------------------------------------------------------------------------------------------
}
