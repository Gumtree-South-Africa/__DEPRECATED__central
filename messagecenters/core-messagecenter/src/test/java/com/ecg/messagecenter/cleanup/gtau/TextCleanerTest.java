package com.ecg.messagecenter.cleanup.gtau;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TextCleanerTest {
    @Test
    public void cleanupText() throws Exception {
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

    @Test
    public void cleanupMarkupCodesInMessage() {
        String result = TextCleaner.cleanupText("blockquote, div.yahoo_quoted margin-left: 0 !important; " +
                "border-left:1px #715FFA solid !important; padding-left:1ex !important; background-color:white " +
                "!important; Hi Rodney. I collected the mirror but I just noticed when i got home and tried to adjust " +
                "the mirror that the left hand side adjustment has no nut holding it in? If you happen across a small " +
                "black threaded nut that fits in an Ikea hole for the threaded bolt to screw into, I would appreciate " +
                "you keeping it and maybe mailing it to me? I wish I found the issue at your place as I wouldn't have " +
                "bought it as it is. I hope I can find that missing piece...\n" +
                "On Tuesday, February 6, 2018, 10:35 AM, Rodney via Gumtree wrote:\n" +
                "Ok.. perfect see than..");
        assertEquals(" Hi Rodney. I collected the mirror but I just noticed when i got home and tried to adjust " +
                "the mirror that the left hand side adjustment has no nut holding it in? If you happen across a small " +
                "black threaded nut that fits in an Ikea hole for the threaded bolt to screw into, I would appreciate " +
                "you keeping it and maybe mailing it to me? I wish I found the issue at your place as I wouldn't have " +
                "bought it as it is. I hope I can find that missing piece...\n" +
                "On Tuesday, February 6, 2018, 10:35 AM, Rodney via Gumtree wrote:\n" +
                "Ok.. perfect see than..", result);


        result = TextCleaner.cleanupText("blockquote, div.yahoo_quoted margin-left: 0 !important; " +
                "border-left:1px #715FFA solid !important; padding-left:1ex !important; background-color:white !important; Ok\n" +
                "Sent from Yahoo Mail for iPhone\n" +
                "On Tuesday, February 6, 2018, 8:40 AM, Shan via Gumtree wrote:\n" +
                "Thanks Brett. Sorry i missed your message. Can you please come tomorrow to come have a look? I will be home in the evening.\n" +
                "Regards");
        assertEquals(" Ok\n" +
                "Sent from Yahoo Mail for iPhone\n" +
                "On Tuesday, February 6, 2018, 8:40 AM, Shan via Gumtree wrote:\n" +
                "Thanks Brett. Sorry i missed your message. Can you please come tomorrow to come have a look? I will be home in the evening.\n" +
                "Regards", result);
    }


    private static void assertEqualsIgnoreLineEnding(String message, String expected, String actual) {
        Assert.assertEquals(message, expected.replaceAll("(\\r?\\n)+", "\n"), actual.replaceAll("(\\r?\\n)+", "\n"));
    }

    private String fileName(String fileName) {
        return fileName.replaceAll("\\..*", "");
    }
}
