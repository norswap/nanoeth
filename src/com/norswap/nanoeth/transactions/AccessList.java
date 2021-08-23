package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.StorageKey;
import com.norswap.nanoeth.rlp.IllegalRLPAccess;
import com.norswap.nanoeth.rlp.RLP;
import norswap.utils.NArrays;
import java.util.Arrays;

public final class AccessList {

    // ---------------------------------------------------------------------------------------------

    /** An empty access list. */
    public static final AccessList EMPTY = new AccessList();

    // ---------------------------------------------------------------------------------------------

    /**
     * Associates an address with a collection of storage key within that account's storage tree.
     */
    public static final class AddressKeys {
        public final Address address;
        public final StorageKey[] keys;

        public AddressKeys (Address address, StorageKey[] keys) {
            this.address = address;
            this.keys = keys;
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * A collection of addresses along with associated storage keys within that account's storage
     * tree.
     */
    public final AddressKeys[] addressKeys;

    // ---------------------------------------------------------------------------------------------

    public AccessList (AddressKeys... addressKeys) {
        this.addressKeys = addressKeys;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Parses the given RLP sequence into the access list.
     *
     * @throws IllegalTransactionFormatException if the RLP sequence does not properly encode an
     * access list
     */
    public static AccessList from (RLP seq) throws IllegalTransactionFormatException {

        if (!seq.isSequence()) throw new IllegalTransactionFormatException(
            "access list requires a sequence, but a byte array was provided instead");

        try {
            return new AccessList(seq.stream()
                .map(it -> new AddressKeys(
                    new Address(it.itemAt(0).bytes()),
                    it.itemAt(1).stream()
                        .map(k -> new StorageKey(k.bytes()))
                        .toArray(StorageKey[]::new)))
                .toArray(AddressKeys[]::new));
        } catch (ArrayIndexOutOfBoundsException | IllegalRLPAccess e) {
            throw new IllegalTransactionFormatException(
                "Access list items must have format [address, [key, ...]]");
        }
    }

    // ---------------------------------------------------------------------------------------------

    /** Returns the RLP representation of this access list. */
    public RLP rlp() {
        return RLP.sequence((Object[]) Arrays.stream(addressKeys)
            .map(it ->
                RLP.sequence(
                    RLP.bytes(it.address.bytes),
                    RLP.sequence(Arrays.stream(it.keys)
                        .map(k -> RLP.bytes(k.bytes))
                        .toArray(RLP[]::new))))
            .toArray(RLP[]::new));
    }

    // ---------------------------------------------------------------------------------------------

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessList that = (AccessList) o;
        for (var pair: NArrays.zip(addressKeys, that.addressKeys)) {
            if (!pair.a.address.equals(pair.b.address))
                return false;
            for (var keysPair: NArrays.zip(pair.a.keys, pair.b.keys))
                if (!keysPair.a.equals(keysPair.b))
                    return false;
        }
        return true;
    }

    @Override public int hashCode () {
        var hashes = Arrays.stream(addressKeys)
            .mapToInt(it -> 31 * it.address.hashCode() + Arrays.hashCode(it.keys))
            .toArray();
        return Arrays.hashCode(hashes);
    }

    @Override public String toString () {
        return Arrays.toString(
            Arrays.stream(addressKeys)
                .map(it -> it.address.toString() + " :: " + Arrays.toString(it.keys))
                .toArray(String[]::new));
    }

    // ---------------------------------------------------------------------------------------------
}
