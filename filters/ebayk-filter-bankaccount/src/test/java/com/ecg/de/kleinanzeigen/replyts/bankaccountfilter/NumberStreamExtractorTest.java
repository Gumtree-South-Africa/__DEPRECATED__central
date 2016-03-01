package com.ecg.de.kleinanzeigen.replyts.bankaccountfilter;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;

public class NumberStreamExtractorTest {


    @Test
    public void extractsOneNumberToOneGroup() {

        assertEquals(newArrayList("1234567"), toStream("1234567"));

    }

    @Test
    public void skipsUnlimitedNumberOfWhitespace() {
        assertEquals(newArrayList("123456"), toStream("      1    2                        3              4    \n5       \t6       "));
    }

    @Test
    public void skipsDefinedNumberOfTextCharacters() {
        assertEquals(newArrayList("123456"), toStream("---1-----2-----3-----4--5-6--"));
    }

    @Test
    public void breaksBetweenLargerTextBlocks() {
        assertEquals(newArrayList("123", "456"), toStream("123_______________456"));

    }

    @Test
    public void handlesLargeTextBlocksInSurrounding() {
        assertEquals(newArrayList("123", "456"), toStream("________________123_______________456___________________"));

    }

    private List<String> toStream(String input) {
        return new NumberStreamExtractor(5, input).extractStream().getItems();
    }
}
