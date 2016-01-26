package com.ecg.replyts.core.api.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StorageUnitTest {


    @Test
    public void leavesBytes() {
        assertEquals(13l, StorageUnit.B.toByte(13));
    }

    @Test
    public void convertsKb() {
        assertEquals(8l * 1024, StorageUnit.KB.toByte(8));
    }

    @Test
    public void convertsMb() {
        assertEquals(9l * 1024 * 1024, StorageUnit.MB.toByte(9));
    }

}
