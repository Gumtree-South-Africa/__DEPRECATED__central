package com.ecg.messagecenter.it.util;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.Security;

/**
 * Encrypts message id's for use in "Message-ID" and "In-Reply-To" headers.
 */
public class MessageIdHeaderEncryption {

    /**
     * Owners of this key can see original message ids and because these
     * are sequential can therefore estimate the number of messages sent
     * through the platform.
     * A leak of this key is not serious enough to make this configurable.
     */
    private static final byte[] KEY_BYTES =
                    new byte[] {56, -72, 54, 122, 32, -29, 75, -109, 101, -127, -102, -84, 101,
                                    -127, -102, -84};
    private static final Charset US_ASCII = Charset.forName("US-ASCII");

    private static final SecretKeySpec KEY_SPEC;

    static {
        Security.addProvider(
                        new org.bouncycastle.jce.provider.BouncyCastleProvider()); // NOSONAR - PMD false positive bug in latest sonar ("Avoid Thread Group")
        KEY_SPEC = new SecretKeySpec(KEY_BYTES, "AES");
    }

    /**
     * Encrypt a message id.
     * <p>
     * WARNING: due to random padding this is not a pure function, it will return different results
     * for each invocation.
     *
     * @param messageId the message id to encrypt,
     *                  may not have leading or trailing spaces,
     *                  must have US-ASCII characters only
     * @return MIME header safe encrypted message id
     */
    public String encrypt(String messageId) {
        if (!messageId.trim().equals(messageId)) {
            throw new IllegalArgumentException(
                            "Trailing/leading spaces in message id are not allowed");
        }
        byte[] input = messageId.getBytes(US_ASCII);
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/ISO10126Padding", "BC");
            cipher.init(Cipher.ENCRYPT_MODE, KEY_SPEC);
            byte[] cipherText = new byte[cipher.getOutputSize(input.length)];
            int ctLength = cipher.update(input, 0, input.length, cipherText, 0);
            cipher.doFinal(cipherText, ctLength);
            return toBase36String(cipherText);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decrypt the encrypted MIME safe header as produced by {@link #encrypt(String)}.
     *
     * @param encryptedMessageId the encrypted message id
     * @return the decrypted message id
     */
    public String decrypt(String encryptedMessageId) {
        byte[] cipherText = toBytes(encryptedMessageId);
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/ISO10126Padding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, KEY_SPEC);
            byte[] plainText = new byte[cipher.getOutputSize(cipherText.length)];
            int ptLength = cipher.update(cipherText, 0, cipherText.length, plainText, 0);
            cipher.doFinal(plainText, ptLength);
            return new String(plainText, US_ASCII).trim();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String toBase36String(byte[] bytes) {
        // Prepend a marker byte with highest bit set to 0.
        // This will make the BigInt positive so that the base36
        // representation doesn't have a minus symbol.
        byte[] withMarkerByte = new byte[bytes.length + 1];
        withMarkerByte[0] = 127;
        // Manual copy of array because this is a very short array.
        //noinspection ManualArrayCopy
        for (int i = 0; i < bytes.length; i++) { // NOSONAR
            withMarkerByte[i + 1] = bytes[i]; // NOSONAR
        }
        return new BigInteger(withMarkerByte).toString(36);
    }

    private byte[] toBytes(String base36String) {
        byte[] withMarkerByte = new BigInteger(base36String, 36).toByteArray();
        byte[] bytes = new byte[withMarkerByte.length - 1];
        // Manual copy of array because this is a very short array.
        //noinspection ManualArrayCopy
        for (int i = 0; i < bytes.length; i++) { // NOSONAR
            bytes[i] = withMarkerByte[i + 1]; // NOSONAR
        }
        return bytes;
    }

}
