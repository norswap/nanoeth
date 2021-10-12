package com.norswap.nanoeth.trees.verkle;

import com.norswap.nanoeth.crypto.Crypto;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.Hashing;
import com.norswap.nanoeth.utils.Randomness;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

/**
 * A zero-knowledge proof showing that we know the values of a set of vectors whose {@link
 * Crypto#pedersenCommitment(BigInteger, BigInteger[], ECPoint[]) commitments} have previously been
 * sent to the verifier.
 * <p>
 * Besides zero-knowledged, the nice point of this proof is that it lets you prove knowledge of many
 * vectors of size N while only sending a single vector of size N.
 * <p>
 * Also see {@link NonHidingVectorsProof} for a lighter but less-hiding version of this proof.
 */
public class VectorsProof {

    // ---------------------------------------------------------------------------------------------

    /** The vector of basis points used for the proof. This is public information. */
    public final ECPoint[] basis;

    /** The size of the vectors whose knowledge we are proving. */
    public final int vectorSize;

    /** A commitment to an additional vector, necessary to ensure hiding. */
    public final ECPoint extraVectorCommitment;

    /**
     * A vector sent in response to the challenge, summing over the inner product of [the proven
     * vectors & the extra vector] with a set of powers of the challenge.
     */
    public final BigInteger[] answerVector;

    /**
     * A scalar sent in response to the challenge, the inner product of a random vectors with a set
     * of powers of the challenge.
     */
    public final BigInteger answerScalar;

    // ---------------------------------------------------------------------------------------------

    /**
     * @param basis cf. {@link #basis}
     * @param vectorSize cf. {@link #vectorSize}
     * @param randoms The random values used to create the commitments to the {@code vectors}.
     * @param vectors A set of vectors that have been previously {@link
     *  Crypto#pedersenCommitment(BigInteger, BigInteger[], ECPoint[]) commited to} using the random
     *  value in {@code randoms}.
     */
    public VectorsProof (ECPoint[] basis, int vectorSize, BigInteger[] randoms, BigInteger[][] vectors) {
        assert randoms.length == vectors.length;

        this.basis = basis;
        this.vectorSize = vectorSize;
        var random = Randomness.randomInteger();
        var extraVector = Randomness.randomIntegers(vectorSize);
        extraVectorCommitment = Crypto.pedersenCommitment(random, extraVector, basis);

        // use the fiat-shamir heuristic to make this proof non-interactive
        var challenge = computeChallenge(extraVectorCommitment);

        // TL;DR: answerVector[i] = extraVector[i] + sum{v}(challenge.pow(v + 1) * vectors[v][i])
        answerVector = extraVector.clone();
        for (int v = 0; v < vectors.length; ++v) {
            assert vectors[v].length == vectorSize;
            var factor = challenge.pow(v + 1);
            for (int i = 0; i < vectorSize; i++)
                answerVector[i] = answerVector[i].add(factor.multiply(vectors[v][i]));
        }

        // TL;DR: answerScalar = random + sum{i}(challenge.pow(i + 1) * randoms[i])
        var answerScalar = random;
        for (int i = 0; i < randoms.length; i++) {
            var factor = challenge.pow(i + 1);
            answerScalar = answerScalar.add(factor.multiply(randoms[i]));
        }
        this.answerScalar = answerScalar;
    }

    // ---------------------------------------------------------------------------------------------

    /** Computes the challenge, derived from the extra vector commitment. */
    private static Natural computeChallenge (ECPoint extraVectorCommitment) {
        return new Natural(Hashing.keccak(extraVectorCommitment.getEncoded(true)).bytes);
    }

    // ---------------------------------------------------------------------------------------------

    public boolean verify (ECPoint[] commitments) {
        var challenge = computeChallenge(extraVectorCommitment);

        // TL;DR: sumLeft = extraVectorCommitment + sum{i}(challenge.pow(i + 1) * commitment[i])
        var sumLeft = extraVectorCommitment;
        for (int i = 0; i < commitments.length; i++)
            sumLeft = sumLeft.add(commitments[i].multiply(challenge.pow(i + 1)));

        // TL;DR: sumRight = basis[0] * answerScalar + sum{i}(basis[i + 1] * answerVector[i])
        var sumRight = basis[0].multiply(answerScalar);
        for (int i = 0; i < vectorSize; i++)
            sumRight = sumRight.add(basis[i + 1].multiply(answerVector[i]));

        return sumLeft.equals(sumRight);
    }

    // ---------------------------------------------------------------------------------------------
}
