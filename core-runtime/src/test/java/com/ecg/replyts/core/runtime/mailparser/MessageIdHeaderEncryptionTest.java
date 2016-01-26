package com.ecg.replyts.core.runtime.mailparser;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MessageIdHeaderEncryption}.
 */
public class MessageIdHeaderEncryptionTest {

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        MessageIdHeaderEncryption encryption = new MessageIdHeaderEncryption();
        List<String> testData = randomStrings(100, new Random(System.currentTimeMillis()), 1, 50);
        for (String test : testData) {
            String testString = test.trim();
            String encrypted = encryption.encrypt(testString);
            assertFalse("encrypted must differ from plain text", encrypted.equals(testString));
            assertTrue("encrypted must be MIME header safe", encrypted.matches("[0-9a-z]+"));
            assertEquals("decryption must equal plain text", testString, encryption.decrypt(encrypted));
        }
    }


    private List<String> randomStrings(int count, Random random, int minLength, int maxLength) {
        List<String> strings = new ArrayList<String>(count);
        for (int i = 0; i < count; i++) {
            strings.add(randomUsAsciiString(random, minLength, maxLength));
        }
        return strings;
    }

    private String randomUsAsciiString(final Random random, final int minLength, final int maxLength) {
        final int length = random.nextInt(maxLength - minLength) + minLength;
        final char[] chars = new char[length];
        for (int i = 0, x = chars.length; i < x; ) {
            do {
                // ONLY US-ASCII characters
                final int cp = random.nextInt(0x7F + 1);
                if (!Character.isDefined(cp))
                    continue;
                final char[] chs = Character.toChars(cp);
                if (chs.length > x - i)
                    continue;
                for (final char ch : chs)
                    chars[i++] = ch;
                break;
            } while (true);
        }

        return new String(chars);
    }

}
