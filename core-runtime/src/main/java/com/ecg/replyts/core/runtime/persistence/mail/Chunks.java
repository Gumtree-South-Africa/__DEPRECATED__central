package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.util.StorageSize;
import com.ecg.replyts.core.runtime.persistence.mail.Chunks.Chunk;
import com.google.common.base.Objects;

import java.util.Arrays;
import java.util.Iterator;

class Chunks implements Iterable<Chunk> {

    private final byte[] payload;
    private final StorageSize chunkSize;

    public Chunks(byte[] payload, StorageSize chunkSize) { // NOSONAR
        this.payload = payload;
        this.chunkSize = chunkSize;
    }

    @Override
    public Iterator<Chunk> iterator() {
        return new ChunkIterator();
    }

    public int count() {
        return (int) Math.ceil(((double) payload.length) / (double) chunkSize.byteValue());
    }

    static class Chunk {
        private final int index;
        private final byte[] contents;

        Chunk(int index, byte[] contents) { // NOSONAR
            this.index = index;
            this.contents = contents;
        }

        public int getIndex() {
            return index;
        }

        public byte[] getContents() {
            return contents;
        }

        public String toString() {
            return "Chunk " + index + ", size: " + contents.length + ", hash: " + hashCode();
        }

        @Override
        public boolean equals(Object o) {

            if (o == null) {
                return false;
            }
            if (o instanceof Chunk) {
                return Objects.equal(((Chunk) o).index, this.index) && Arrays.equals(this.contents, ((Chunk) o).contents);

            }
            return false;

        }

        @Override
        public int hashCode() {
            return Objects.hashCode(index, contents);
        }
    }

    private class ChunkIterator implements Iterator<Chunk> {

        private int currentIndex = -1;

        @Override
        public boolean hasNext() {
            return toByteOffset() < payload.length;
        }

        @Override
        public Chunk next() {
            currentIndex++;
            return new Chunk(currentIndex, Arrays.copyOfRange(payload, fromByteOffset(), toByteOffset()));
        }

        private int toByteOffset() {
            return Math.min(payload.length, (currentIndex + 1) * chunkSize.byteValue());
        }

        private int fromByteOffset() {
            return currentIndex * chunkSize.byteValue();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove is not supported - this is a view on a byte array");
        }
    }
}
