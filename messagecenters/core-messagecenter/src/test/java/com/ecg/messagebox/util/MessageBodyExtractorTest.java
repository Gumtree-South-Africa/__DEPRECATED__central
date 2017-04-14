package com.ecg.messagebox.util;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.ecg.messagebox.util.MessageBodyExtractor.END_OF_TEXT;
import static com.ecg.messagebox.util.MessageBodyExtractor.START_OF_TEXT;

public class MessageBodyExtractorTest {


    @Test
    public void basicTextTest() {

        String msg = String.format("header%sbody%sfooter", START_OF_TEXT, END_OF_TEXT);
        String result = MessageBodyExtractor.extractBodyMarkedByNonPrintableChars(msg);
        Assert.assertTrue(result.equals("body"));
    }

    @Test
    public void multilineTextTest() {

        String msg = String.format("header%sbo\ndy%sfooter", START_OF_TEXT, END_OF_TEXT);
        String result = MessageBodyExtractor.extractBodyMarkedByNonPrintableChars(msg);
        Assert.assertTrue(result.equals("bo\ndy"));
    }

    @Test
    public void realTextTest() throws IOException {

        String msg = loadFileAsString("/com/ecg/messagecenter/util/contactMessage.txt");
        String result = MessageBodyExtractor.extractBodyMarkedByNonPrintableChars(msg);

        String expected = loadFileAsString("/com/ecg/messagecenter/util/contactMessageTrimmed.txt");
        Assert.assertTrue(result.equals(expected));
    }

    private String loadFileAsString(String fileName) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(fileName)) {
            return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        }
    }

}