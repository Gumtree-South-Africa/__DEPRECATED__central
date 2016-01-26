package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.runtime.persistence.GZip;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class GZipTest {

    private static final String DATA = "fooooobar this is a long text with some repeated words in this long text with repeated words so that gzipping actually shows results";
    private final byte[] payload = DATA.getBytes();

    private final byte[] compressedPayload = {31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 93, -54, 65, 14, -128, 32, 12, 5,
            -47, -85, -4, -85, 85, -87, 64, -126, -108, -48, 18, -44, -45, 91, -29, -114, -55, 44, -33, 33, 95, 27, 117,
            88, -54, 10, -97, 80, -92, 70, 24, 95, -122, -103, 45, 65, -27, 100, 116, 110, 76, -58, 1, 83, 122, 112, 87,
            127, -65, -48, 69, -87, -72, 34, 67, 124, 114, 107, -39, 37, -19, 54, -88, -108, 27, -102, 100, -86, 115, 29,
            -59, -12, 5, -78, 32, -41, -11, -124, 0, 0, 0};

    @Test
    public void zipsPayload() {
        byte[] compressed = GZip.zip(payload);
        assertArrayEquals(compressedPayload, compressed);
    }

    @Test
    public void unzipsCompressedPayload() {
        assertEquals(DATA, new String(GZip.unzip(compressedPayload)));
    }

    @Test
    public void canUnzipItsOwnZippedContents() {
        byte[] unzipped = GZip.unzip(GZip.zip(payload));
        assertArrayEquals(payload, unzipped);
    }

}
