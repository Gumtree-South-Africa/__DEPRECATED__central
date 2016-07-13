package com.ecg.messagecenter.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmailHeaderFolderTest {

    private static final String SIMPLE_HEADER_VALUE_UNFOLDED = "abc \n \n \n def";
    private static final String SIMPLE_HEADER_VALUE_FOLDED = "abc \\n \\n \\n def";

    private static final String COMPLEX_HEADER_VALUE_UNFOLDED = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi \n abcdefghi";
    private static final String COMPLEX_HEADER_VALUE_FOLDED = "abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi abcdefghi \\n\r\n" +
            " abcdefghi";

    @Test
    public void unfoldHeaderValues() {

        assertEquals(null,
                EmailHeaderFolder.unfold(null));

        assertEquals("abc \n \n \n def",
                EmailHeaderFolder.unfold("abc \\n \\n \\n def"));

        assertEquals(SIMPLE_HEADER_VALUE_UNFOLDED,
                EmailHeaderFolder.unfold(SIMPLE_HEADER_VALUE_FOLDED));

        assertEquals(COMPLEX_HEADER_VALUE_UNFOLDED,
                EmailHeaderFolder.unfold(COMPLEX_HEADER_VALUE_FOLDED));
    }
}
