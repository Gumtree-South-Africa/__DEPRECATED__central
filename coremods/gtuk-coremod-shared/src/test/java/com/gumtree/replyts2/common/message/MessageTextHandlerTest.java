package com.gumtree.replyts2.common.message;

import com.ecg.gumtree.replyts2.common.message.MessageTextHandler;
import com.ecg.replyts.app.Mails;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageTextHandlerTest {
    private static final Logger LOG = LoggerFactory.getLogger(MessageTextHandlerTest.class);


    public static class EmailTestCase {
        String expected;
        String actual;

        EmailTestCase(String expected, String actual) {
            this.expected = expected;
            this.actual = actual;
        }

        public static EmailTestCase aTestCase(String expected, String actual) {
            return new EmailTestCase(expected, actual);
        }
    }

    @Test
    public void testValidMails() throws Exception {
        File mailFolder = new File(getClass().getResource("/mailReceived").getFile());
        Map<String, String> expected = Maps.newHashMap();
        FileInputStream fin;
        long start, end, duration;
        String fileName, result;

        File[] mails = mailFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("eml");
            }


        });
        File[] texts = mailFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith("txt");
            }
        });
        for (File text : texts) {
            expected.put(fileName(text.getName()), IOUtils.toString(new FileInputStream(text)));
        }

        for (File f : mails) {
            fin = new FileInputStream(f);
            fileName = fileName(f.getName());
            try {
                List<String> parts = new Mails().readMail(ByteStreams.toByteArray(fin)).getPlaintextParts();
                start = System.currentTimeMillis();
                result = MessageTextHandler.remove(parts.get(0));
                end = System.currentTimeMillis();
                duration = end - start;
                assertTrue("Long running regex, Length: " + result.length() + " Time: " + duration + " File:[" + fileName + "]", duration < 30 * 10);
                if (expected.containsKey(fileName)) {
                    assertEqualsIgnoreLineEnding("File '" + f.getAbsolutePath() + "'", expected.get(fileName), result);
                }
            } catch (Exception e) {
                throw new RuntimeException("Mail " + f.getAbsolutePath() + " not parseable", e);
            } finally {
                fin.close();
            }
        }
    }

    private static void assertEqualsIgnoreLineEnding(String message, String expected, String actual) {
        assertEquals(message, expected.replaceAll("\r\n", "\n"), actual.replaceAll("\r\n", "\n"));
    }

    private String fileName(String fileName) {
        return fileName.replaceAll("\\..*", "");
    }
}
