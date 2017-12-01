package com.ecg.replyts.core.runtime;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.spi.CharsetProvider;
import java.util.Iterator;
import java.util.Map;

/**
 * Adds support for charsets that aren't handled natively by the JVM, but are
 * actually just aliases of other supported ones.
 */
public class CharsetAliaser extends CharsetProvider {
    private final Map<String, Charset> aliasToTarget;
    private final static String WRONG_CP_ENCODING = "cp-";

    public CharsetAliaser() {
        this.aliasToTarget = ImmutableSortedMap.of(
                // Not an exact alias, but not used often enough to warrant writing our own mapper
                // At kijiji.ca only auto-responders have used this so far
                "ansi_x3.110-1983", createAliasedCharset("ansi_x3.110-1983", "iso-8859-1"),
                "cp932", createAliasedCharset("cp932", "Windows-31J"),
                "iso-8859-8-i", createAliasedCharset("iso-8859-8-i", "iso-8859-8")
        );
    }

    @Override
    public Iterator<Charset> charsets() {
        return aliasToTarget.values().iterator();
    }

    /**
     * ebayk & gtau has emails sent in CP-XXX encoding (e.g. CP-850), which Java knows as CPXXX encoding,
     * This method maps the former to the latter.
     *
     * @param charsetName
     * @return
     */
    @Override
    public Charset charsetForName(String charsetName) {
        if (charsetName != null) {

            charsetName = charsetName.trim().toLowerCase();
            if (charsetName.startsWith(WRONG_CP_ENCODING)) {

                charsetName = charsetName.replace("-", "");
                return Charset.forName(charsetName);
            }
        }

        return aliasToTarget.get(charsetName);
    }

    private Charset createAliasedCharset(String aliasName, String targetName) {
        Preconditions.checkNotNull(aliasName);
        Preconditions.checkNotNull(targetName);

        final Charset targetCharset = Charset.forName(targetName);

        return new Charset(aliasName, null) {
            @Override
            public boolean contains(Charset cs) {
                return targetCharset.contains(cs);
            }

            @Override
            public CharsetDecoder newDecoder() {
                return targetCharset.newDecoder();
            }

            @Override
            public CharsetEncoder newEncoder() {
                return targetCharset.newEncoder();
            }
        };
    }
}
