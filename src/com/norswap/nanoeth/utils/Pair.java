package com.norswap.nanoeth.utils;

import java.util.Objects;

/** Simple object pair. */
public final class Pair<A, B> {

    public final A fst;
    public final B snd;

    private Pair (A fst, B snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public static <A, B> Pair<A, B> of (A fst, B snd) {
        return new Pair<>(fst, snd);
    }

    @Override public boolean equals (Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        var pair = (Pair<?, ?>) o;
        return Objects.equals(fst, pair.fst) && Objects.equals(snd, pair.snd);
    }

    @Override public int hashCode() {
        return Objects.hash(fst, snd);
    }

    @Override public String toString() {
        return "(" + fst + ", " + snd + ")";
    }
}
