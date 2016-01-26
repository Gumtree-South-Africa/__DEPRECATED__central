package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.util.StorageSize;
import com.ecg.replyts.core.api.util.StorageUnit;
import com.ecg.replyts.core.runtime.persistence.mail.Chunks.Chunk;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ChunksTest {
    byte[] eightBytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
    byte[] nineBytes = new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8};

    @Test
    public void countIsCorrectOnFitChunks() {
        assertEquals(4, new Chunks(eightBytes, new StorageSize(2, StorageUnit.B)).count());
    }

    @Test
    public void countIsCorrectOnFitSingleChunkChunkIsBigger() {
        Chunks chunks = new Chunks(eightBytes, new StorageSize(10, StorageUnit.MB));

        assertEquals(1, chunks.count());
        Iterator<Chunk> it = chunks.iterator();

        assertTrue(it.hasNext());
        assertArrayEquals(eightBytes, it.next().getContents());

        assertFalse(it.hasNext());
    }

    @Test
    public void countIsCorrectOnFitSingleChunkSameLength() {
        Chunks chunks = new Chunks(eightBytes, new StorageSize(eightBytes.length, StorageUnit.B));

        assertEquals(1, chunks.count());
        Iterator<Chunk> it = chunks.iterator();

        assertTrue(it.hasNext());
        assertArrayEquals(eightBytes, it.next().getContents());

        assertFalse(it.hasNext());
    }

    @Test
    public void countIsCeiled() {
        assertEquals(5, new Chunks(nineBytes, new StorageSize(2, StorageUnit.B)).count());
    }

    @Test
    public void chunksAreCorrectlyCutOnFitChunks() {
        Chunks chunks = new Chunks(eightBytes, new StorageSize(2, StorageUnit.B));

        Iterator<Chunk> it = chunks.iterator();

        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{0, 1}, it.next().getContents());
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{2, 3}, it.next().getContents());
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{4, 5}, it.next().getContents());
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{6, 7}, it.next().getContents());

        assertFalse(it.hasNext());
    }

    @Test
    public void chunksAreCorrectlyCutOnMisfitChunks() {
        Chunks chunks = new Chunks(nineBytes, new StorageSize(2, StorageUnit.B));
        Iterator<Chunk> it = chunks.iterator();
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{0, 1}, it.next().getContents());
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{2, 3}, it.next().getContents());
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{4, 5}, it.next().getContents());
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{6, 7}, it.next().getContents());
        assertTrue(it.hasNext());
        assertArrayEquals(new byte[]{8}, it.next().getContents());
        assertFalse(it.hasNext());
    }

    @Test
    public void chunkIndexesAreCorrect() {
        Chunks chunks = new Chunks(eightBytes, new StorageSize(2, StorageUnit.B));
        Iterator<Chunk> it = chunks.iterator();

        assertEquals(0, it.next().getIndex());
        assertEquals(1, it.next().getIndex());
        assertEquals(2, it.next().getIndex());
        assertEquals(3, it.next().getIndex());
    }
}
