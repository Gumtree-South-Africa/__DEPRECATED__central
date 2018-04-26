package com.gumtree.replyts2.common.message;

import com.ecg.gumtree.replyts2.common.message.MessageTextHandler;
import com.ecg.replyts.app.Mails;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageTextHandlerTest {
    @Test
    public void testValidMails() throws Exception {

        List<File> files = Files.list(Paths.get("src/test/resources/mailReceived")).map(Path::toFile).collect(toList());

        List<File> mails = files.stream().filter(file -> file.getName().endsWith(".eml")).collect(toList());
        List<File> texts = files.stream().filter(file -> file.getName().endsWith(".txt")).collect(toList());

        assertTrue(!mails.isEmpty());
        assertTrue(!texts.isEmpty());

        Map<String, String> expected = Maps.newHashMap();

        for (File text : texts) {
            expected.put(fileName(text.getName()), IOUtils.toString(new FileInputStream(text)));
        }

        for (File f : mails) {
            FileInputStream fin = new FileInputStream(f);
            String fileName = fileName(f.getName());
            try {
                List<String> parts = Mails.readMail(ByteStreams.toByteArray(fin)).getPlaintextParts();
                long start = System.currentTimeMillis();
                String result = MessageTextHandler.remove(parts.get(0));
                long end = System.currentTimeMillis();
                long duration = end - start;
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
