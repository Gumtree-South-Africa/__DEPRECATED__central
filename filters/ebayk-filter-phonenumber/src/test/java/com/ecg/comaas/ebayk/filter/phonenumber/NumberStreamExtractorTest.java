package com.ecg.comaas.ebayk.filter.phonenumber;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NumberStreamExtractorTest {

    @Test
    public void extractsOneNumberToOneGroup() {
        assertThat(toStream("1234567")).containsExactly("1234567");
    }

    @Test
    public void skipsUnlimitedNumberOfWhitespace() {
        assertThat(toStream("      1    2                        3              4     5       \t6       ")).containsExactly("123456");
    }

    @Test
    public void newGroupOnBreakingLF() {
        assertThat(toStream("12345\n6789")).containsExactly("12345","6789");
    }

    @Test
    public void newGroupOnBreakingCR() {
        assertThat(toStream("12345\r6789")).containsExactly("12345","6789");
    }

    @Test
    public void newGroupOnBreakingLFCR() {
        assertThat(toStream("12345\r\n\r\n6789")).containsExactly("12345","6789");
    }

    @Test
    public void skipsDefinedNumberOfTextCharacters() {
        assertThat(toStream("---1-----2-----3-----4--5-6--")).containsExactly("123456");
    }

    @Test
    public void breaksBetweenLargerTextBlocks() {
        assertThat(toStream("123_______________456")).containsExactly("123", "456");
    }

    @Test
    public void handlesLargeTextBlocksInSurrounding() {
        assertThat(toStream("________________123_______________456___________________")).containsExactly("123", "456");
    }

    @Test
    public void emptyIfGroupToShort() {
        assertThat(new NumberStreamExtractor(5, 6).extractStream("(123)45").getItems()).isEmpty();
    }

    @Test
    public void ignoreIfGroupToShort() {
        assertThat(new NumberStreamExtractor(5, 6).extractStream("(123)45------(7890)12").getItems()).containsExactly("789012");
    }

    @Test
    public void interpretateOwithZero() {
        assertThat(toStream("o1234O")).containsExactly("012340");
    }

    @Test
    public void interpretateIlwithOne() {
        assertThat(toStream("I1234l")).containsExactly("112341");
    }

    @Test
    public void ignoreLowerCaseI() {
        assertThat(toStream("i1234l")).containsExactly("12341");
    }

    private List<String> toStream(String input) {
        return new NumberStreamExtractor(5, 3).extractStream(input).getItems();
    }
}
