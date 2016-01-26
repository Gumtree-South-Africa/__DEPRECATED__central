package com.ecg.replyts.core.api.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StorageSizeTest {

    @Test
    public void sameSizesEqualInDifferentUnits() {
        assertEquals(new StorageSize(1024, StorageUnit.B), new StorageSize(1, StorageUnit.KB));
    }

    @Test
    public void isSmallerThanWorks() {
        assertTrue(new StorageSize(1, StorageUnit.KB).isSmallerThan(new StorageSize(1, StorageUnit.MB)));
    }

    @Test
    public void isBiggerThanWorks() {
        assertTrue(new StorageSize(1, StorageUnit.MB).isBiggerThan(new StorageSize(1, StorageUnit.KB)));
    }
}
