package com.norswap.nanoeth.signature;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.Natural;
import com.norswap.nanoeth.utils.Hashing;
import org.bouncycastle.math.ec.ECPoint;
import java.math.BigInteger;
import java.util.Arrays;

import static com.norswap.nanoeth.crypto.Curve.SECP256K1;

/**
 * Signature-related utilities that don't obviously belong in other classes.
 */
public final class SignatureUtils {
    private SignatureUtils() {}

    // ---------------------------------------------------------------------------------------------

    /** Note that in SECP256K1, N (curve order) and n (subgroupe order) are identical. */
    static final BigInteger SECP256K1_HALF_N =
            SECP256K1.n().shiftRight(1); // n/2

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns the address of the Ethereum account associated with the public key, which is formed
     * by the 20 rightmost byte of the hash of the public key's encoding.
     */
    public static Address address (ECPoint publicKey) {
        byte[] bytes = publicKey.getEncoded(false);
        // strip header byte that indicates that the representation is uncompressed
        bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        byte[] hash = Hashing.keccak(bytes).bytes;
        return new Address(Arrays.copyOfRange(hash, 12, 32));
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Canonicalizes the {@code s} signature value if needed.
     *
     * <p>Canonicalization is required because for every signature (r,s) the signature (r, -s (mod
     * n)) is a valid signature of the same message. See signature package README for more
     * information.
     *
     * <p>This change was introduced in EIP-2 (Homestead). We apply it to all transactions, since
     * canonicalized transactions are valid before Homestead as well.
     */
    public static Natural canonicalizeS (Natural s) {
        return s.compareTo(SECP256K1_HALF_N) > 0
            ? new Natural(SECP256K1.n().subtract(s))
            : s;
    }

    // ---------------------------------------------------------------------------------------------
}
