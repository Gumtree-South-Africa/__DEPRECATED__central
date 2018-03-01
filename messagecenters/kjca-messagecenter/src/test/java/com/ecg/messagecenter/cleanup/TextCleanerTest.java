package com.ecg.messagecenter.cleanup;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;


public class TextCleanerTest {
    @Test
    public void singleLetterAccentedWords_doNotGetRemoved() {
        final String frenchPhrase = "Bonjour! Ceci est une réponse à votre annonce";
        final Text text = new Text(frenchPhrase);
        org.junit.Assert.assertEquals("'à' should not be removed.", text.getAsString(), frenchPhrase);
    }

    @Test
    @Ignore
    public void testValidMails() throws Exception {
        File mailFolder = new File(getClass().getResource("mailReceived").getFile());
        Map<String, String> expected = Maps.newHashMap();
        FileInputStream fin;
        long start, end, duration;
        String fileName, result;

        File[] mails = mailFolder.listFiles((dir, name) -> name.endsWith("eml"));
        File[] texts = mailFolder.listFiles((dir, name) -> name.endsWith("txt"));
        for (File text : texts) {
            expected.put(fileName(text.getName()), IOUtils.toString(new FileInputStream(text)));
        }

        for (File f : mails) {
            fin = new FileInputStream(f);
            fileName = fileName(f.getName());
            try {
                Mail mail = Mails.readMail(ByteStreams.toByteArray(fin));
                Map<String, String> headers = mail.getUniqueHeaders();
                List<String> parts = mail.getPlaintextParts();
                start = System.currentTimeMillis();
                result = TextCleaner.cleanupText(parts.get(0));
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
        Assert.assertEquals(message, expected.replaceAll("\r\n", "\n"), actual.replaceAll("\r\n", "\n"));
    }

    private String fileName(String fileName) {
        return fileName.replaceAll("\\..*", "");
    }
}
