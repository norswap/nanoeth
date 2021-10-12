package com.norswap.nanoeth.trees.verkle;

import com.norswap.nanoeth.crypto.Crypto;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.trees.patricia.PatriciaTree;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;

/**
 * A zero-knowledge proof showing that we know the values of a set of vectors whose {@link
 * Crypto#pedersenCommitment(BigInteger[], ECPoint[]) non-hiding commitments} have previously been
 * sent to the verifier.
 * <p>
 * This enables making a zero-knowledge proof of the vectors, albeit information about the vectors
 * might be leaking by the proof. I don't know the details, but consider that you are already using
 * {@link Crypto#pedersenCommitment(BigInteger[], ECPoint[]) non-hiding commitments}.
 * <p>
 * Beyond zero-knowledge (with above caveat), the nice point of this proof is that it lets you prove
 * knowledge of many vectors of size N while only sending a single vector of size N.
 */
public class NonHidingVectorsProof {

    // ---------------------------------------------------------------------------------------------

    /** The vector of basis points used for the proof. This is public information. */
    public final ECPoint[] basis;

    /** The size of the vectors whose knowledge we are proving. */
    public final int vectorSize;

    /**
     * A vector sent in response to the challenge, summing over the inner product of the proven
     * vectors with a set of powers of the challenge.
     */
    public final BigInteger[] answerVector;

    // ---------------------------------------------------------------------------------------------

    /**
     * @param basis cf. {@link #basis}
     * @param vectorSize cf. {@link #vectorSize}
     * @param vectors A non-empty set of vectors that have been previously {@link
     *  Crypto#pedersenCommitment(BigInteger[], ECPoint[]) commited to}.
     */
    public NonHidingVectorsProof (ECPoint[] basis, int vectorSize, BigInteger[][] vectors) {
        assert vectors.length > 0;
        this.basis = basis;
        this.vectorSize = vectorSize;
        var challenge = computeChallenge();

        // TL;DR: answerVector[i] = extraVector[i] + sum{v}(challenge.pow(v + 1) * vectors[v][i])
        answerVector = vectors[0].clone();
        assert answerVector.length == vectorSize;
        for (int v = 1; v < vectors.length; ++v) {
            assert vectors[v].length == vectorSize;
            var factor = challenge.pow(v);
            for (int i = 0; i < vectorSize; i++)
                answerVector[i] = answerVector[i].add(factor.multiply(vectors[v][i]));
        }
    }

    // ---------------------------------------------------------------------------------------------

    private static Natural computeChallenge() {
        return new Natural(PatriciaTree.EMPTY_TREE_ROOT.bytes);
    }

    // ---------------------------------------------------------------------------------------------

    public boolean verify (ECPoint[] commitments) {
        assert commitments.length > 0;
        var challenge = computeChallenge();

        // TL;DR: sumLeft = sum{i}(challenge.pow(i) * commitment[i])
        var sumLeft = commitments[0];
        for (int i = 1; i < commitments.length; i++)
            sumLeft = sumLeft.add(commitments[i].multiply(challenge.pow(i)));

        // TL;DR: sumRight = sum{i}(basis[i] * answerVector[i])
        var sumRight = basis[0].multiply(answerVector[0]);
        for (int i = 1; i < vectorSize; i++)
            sumRight = sumRight.add(basis[i].multiply(answerVector[i]));

        return sumLeft.equals(sumRight);
    }

    // ---------------------------------------------------------------------------------------------
}
