package com.ecg.replyts.core.api.util;

import java.math.BigInteger;

/**
 * Internal utility that will convert a unsigned base30 value from a signed long value by internally seeing the value as unsigned.
 *
 * Unlike @{link java.lang.Long#toUnsignedString} and {@link com.google.common.primitives.UnsignedLongs#toString(long, int)}, this version
 * <em>does</em> take the upper bit into account.
 *
 */
public final class UnsignedLong {

    private final BigInteger backed;

    private UnsignedLong(BigInteger backed) {
        this.backed = backed;
    }

    /**
     * Create an UnsignedLong from a regular long. The sign bit is treated as the 64th bit.
     *
     * @param l a long
     * @return an unsigned long
     */
    public static UnsignedLong fromLong(long l) {
        byte[] bytes = new byte[9];

        for (int i = 1; i < 9; i++) {
            bytes[i] = (byte) ((l >> ((8 - i) * 8)) & 255);
        }

        return new UnsignedLong(new BigInteger(bytes));
    }

    /**
     * Convert the BigInteger to its base-30 equivalent String
     * @return
     */
    public String toBase30() {
        return Base30.convert(backed);
    }

}
