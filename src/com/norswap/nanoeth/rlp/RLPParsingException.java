package com.norswap.nanoeth.rlp;

import java.util.ArrayDeque;

/**
 * Signals a problem while parsing some data from an RLP layout.
 * <p>
 * Receiving this exception means that the binary data has already successfully been decoded to a
 * {@link RLP} structure — failure to do so results in a {@link IllegalArgumentException} being
 * thrown from {@link RLP#decode(byte[])}. Therefore this exception means the RLP structure does not
 * conform to some domain logic requirements.
 * <p>
 * The exception is enriched with a stack of string ({@link #trace}) used to precisely pinpoint
 * where the parsing problem occured.
 */
public final class RLPParsingException extends Exception {

    // ---------------------------------------------------------------------------------------------

    public final ArrayDeque<String> trace = new ArrayDeque<>();

    // ---------------------------------------------------------------------------------------------

    public RLPParsingException (String message) {
        super(message);
    }

    // ---------------------------------------------------------------------------------------------

    public RLPParsingException (String message, Throwable cause) {
        super(message, cause);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Returns a string containing the initial message, then all the messages in {@link #trace}, in
     * insertion order, separated by newlines.
     */
    public String trace() {
        var b = new StringBuilder(getMessage());
        for (String s: trace) {
            b.append(s).append("\n");
        }
        return b.toString();
    }

    // ---------------------------------------------------------------------------------------------
}
