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

    public CharsetAliaser() {
        this.aliasToTarget = ImmutableSortedMap.of(
                // Not an exact alias, but not used often enough to warrant writing our own mapper
                // At kijiji.ca only auto-responders have used this so far
                "ansi_x3.110-1983", createAliasedCharset("ansi_x3.110-1983", "iso-8859-1")
        );
    }

    @Override
    public Iterator<Charset> charsets() {
        return aliasToTarget.values().iterator();
    }

    @Override
    public Charset charsetForName(String charsetName) {
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
