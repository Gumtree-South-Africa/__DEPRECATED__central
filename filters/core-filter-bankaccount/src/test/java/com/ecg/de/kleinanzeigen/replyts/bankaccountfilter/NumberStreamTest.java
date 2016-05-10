package com.ecg.de.kleinanzeigen.replyts.bankaccountfilter;

import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NumberStreamTest {

    private NumberStream numberStream = new NumberStream(Lists.newArrayList("123456", "1", "654321", "123987", "9898434"));

    @Test
    public void doesNotFindBoth() {
        assertFalse(numberStream.containsBoth("9999", "8888"));

    }

    @Test
    public void findsBothIfTheyEqualSegment() {
        assertTrue(numberStream.containsBoth("123456", "654321"));
    }

    @Test
    public void findsBothIfTheyAreContainedInDifferentSegments() {
        assertTrue(numberStream.containsBoth("123", "398"));
    }

    @Test
    public void returnsFalseIfFindsOnlyOne() {
        assertFalse(numberStream.containsBoth("123", "99999999999"));
    }

    @Test
    public void returnsTrueIfInSameSegmentButDoNotOverlap() {
        assertTrue(numberStream.containsBoth("9898", "434"));
    }

    @Test
    public void returnsFalseIfInSameSegmentButDoOverlap() {
        assertFalse(numberStream.containsBoth("9898", "8434"));
    }


    @Test
    public void returnsTrueIfBothOverlapButAnotheroneFound() {
        assertTrue(numberStream.containsBoth("34", "434"));
        assertTrue(numberStream.containsBoth("434", "34"));

    }
}
