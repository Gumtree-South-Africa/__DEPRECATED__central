package com.ecg.de.kleinanzeigen.replyts.phonenumberfilter;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NumberStreamTest {

    private final NumberStream numberStream = new NumberStream(asList("123456", "1", "654321", "123987", "9898434"));

    @Test
    public void doesNotFind() {
        assertFalse(numberStream.contains("9999"));
    }

    @Test
    public void findsIfEqualSegment() {
        assertTrue(numberStream.contains("123456"));
    }

    @Test
    public void findFragmentInDifferentSegments() {
        assertTrue(numberStream.contains("123"));
    }

    @Test
    public void findFragmentInOneSegment() {
        assertTrue(numberStream.contains("456"));
    }

}
