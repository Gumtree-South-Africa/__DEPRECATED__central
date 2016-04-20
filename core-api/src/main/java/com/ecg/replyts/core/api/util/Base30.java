package com.ecg.replyts.core.api.util;

import java.math.BigInteger;

/**
 * Encodes numbers into base-30, using a digit set consisting of numerals and English consonants (not including Y)
 *
 * Requires input to be non-negative numbers.
 */
class Base30 {
    private static final String DIGITS = "0123456789bcdfghjklmnpqrstvwxz";
    private static final BigInteger BASE_30_RADIX = BigInteger.valueOf(30L);

    static String convert(BigInteger value) {
        if (value.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Base30 only supports positive values");
        }

        if (value.compareTo(BASE_30_RADIX) < 0) {
            return convert(value.longValue());
        }

        final BigInteger[] quotientAndRemainder = value.divideAndRemainder(BASE_30_RADIX);
        return convert(quotientAndRemainder[0]) + convert(quotientAndRemainder[1]);
    }

    static String convert(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Base30 only supports positive values");
        }

        StringBuilder result = new StringBuilder(40);
        while (value > 0) {
            long mod = value % 30;
            value -= mod;
            if (value > 0) {
                value /= 30;
            }
            result = result.insert(0, DIGITS.charAt((int) mod));
        }

        return result.length() == 0 ? "0" : result.toString();
    }
}
