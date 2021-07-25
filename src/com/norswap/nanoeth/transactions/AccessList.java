package com.norswap.nanoeth.transactions;

import com.norswap.nanoeth.data.Address;
import com.norswap.nanoeth.data.StorageKey;
import com.norswap.nanoeth.rlp.RLPBytes;
import com.norswap.nanoeth.rlp.RLPSequence;
import norswap.utils.NArrays;
import norswap.utils.exceptions.NoStackException;
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
     * @throws IllegalTransactionFormatException if the RLP sequence does not properly encode an access list
     */
    public static AccessList from (RLPSequence rlp) throws IllegalTransactionFormatException {
        // TODO improve this mess - using streams?
        var addressKeys = new AddressKeys[rlp.size()];
        for (int i = 0; i < addressKeys.length; ++i) {
            var addressKeySeq = ((RLPSequence) rlp.get(i));
            try {
                if (addressKeySeq.size() != 2)
                    throw new NoStackException();
                var address = new Address(((RLPBytes) addressKeySeq.get(0)).bytes.storage);
                var storageKeysSeq = (RLPSequence) addressKeySeq.get(1);
                var storageKeys = new StorageKey[storageKeysSeq.size()];
                for (int j = 0; j < storageKeys.length; ++j)
                    storageKeys[j] = new StorageKey(((RLPBytes) storageKeysSeq.get(j)).bytes.storage);
                addressKeys[i] = new AddressKeys(address, storageKeys);
            } catch (ClassCastException | NoStackException e) {
                throw new IllegalTransactionFormatException(
                    "Access list items must have format [address, [key, ...]]");
            }
        }
        return new AccessList(addressKeys);
    }

    // ---------------------------------------------------------------------------------------------

    // TODO use norswap-utils

    /**
     * TODO document
     * @return
     */
    public RLPSequence rlp() {
        var addressKeysSeqs = new RLPSequence[addressKeys.length];
        for (int i = 0; i < addressKeys.length; ++i) {
            var address = addressKeys[i].address;
            var keys    = addressKeys[i].keys;

            var keysBytes = new RLPBytes[keys.length];
            for (int j = 0; j < keys.length; ++j)
                keysBytes[j] = RLPBytes.from(keys[j].bytes);

            addressKeysSeqs[i] = RLPSequence.from(
                RLPBytes.from(address.bytes),
                RLPSequence.from(keysBytes));
        }
        return RLPSequence.from(addressKeysSeqs);
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
                .map(it -> it.address.toString() + " :: [" + Arrays.toString(it.keys) + "]")
                .toArray(String[]::new));
    }

    // ---------------------------------------------------------------------------------------------
}
