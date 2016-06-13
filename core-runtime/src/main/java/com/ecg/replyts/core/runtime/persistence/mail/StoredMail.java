package com.ecg.replyts.core.runtime.persistence.mail;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteStreams;
import com.nothome.delta.Delta;
import com.nothome.delta.GDiffPatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * stores inbound and outbound mail in a zipped (no compression) unlzma archive
 */
public class StoredMail {

    public static final String OUTBOUND = "OUTBOUND";
    private static final String OUTBOUND_HASH = "OUTBOUND_HASH";
    public static final String INBOUND = "INBOUND";
    private final byte[] inboundContents;
    private final Optional<byte[]> outboundContents;

    private static final Logger LOG = LoggerFactory.getLogger(StoredMail.class);

    public StoredMail(byte[] inboundContents, Optional<byte[]> outboundContents) { // NOSONAR ignore that the array contents are not copied to an immutable version. mails can be several MBs big
        Preconditions.checkNotNull(inboundContents);
        Preconditions.checkNotNull(outboundContents);
        this.inboundContents = inboundContents;
        this.outboundContents = outboundContents;
    }

    public byte[] compress() {
        return zip();
    }

    public static StoredMail extract(byte[] compressedContents) {
        try {
            ZipInputStream inputStream = unzip(compressedContents);
            byte[] inbound = null, outbound = null, outboundHash = null;
            for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream.getNextEntry()) {
                if (entry.getName().equals(INBOUND)) {
                    inbound = ByteStreams.toByteArray(inputStream);
                } else if (entry.getName().equals(OUTBOUND)) {
                    outbound = patch(inbound, ByteStreams.toByteArray(inputStream));
                } else if (entry.getName().equals(OUTBOUND_HASH)) {
                    outboundHash = ByteStreams.toByteArray(inputStream);
                }
            }

            if (outbound != null && outboundHash != null) {
                boolean sameHash = Arrays.equals(outboundHash, hashOf(outbound));
                if (!sameHash) {
                    LOG.error("stored mail hash file corruption (the outbound mail could not be reconstructed)");
                    outbound = null;
                }
            }

            return new StoredMail(inbound, Optional.fromNullable(outbound));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] patch(byte[] inbound, byte[] bytes) {
        try {
            return new GDiffPatcher().patch(inbound, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] diff() {
        try {
            Delta delta = new Delta();
            delta.setChunkSize(8);
            return delta.compute(inboundContents, outboundContents.get());
        } catch (IOException e) {
            // can's happen, delta uses a byte array input stream internally
            throw new RuntimeException(e);
        }
    }

    private byte[] zip() {

        ByteArrayOutputStream resultBuffer = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(resultBuffer)) {

            zipOutputStream.setMethod(ZipOutputStream.DEFLATED);
            zipOutputStream.setLevel(9);

            zipOutputStream.putNextEntry(new ZipEntry(INBOUND));
            zipOutputStream.write(inboundContents);
            zipOutputStream.closeEntry();

            if (outboundContents.isPresent()) {
                zipOutputStream.putNextEntry(new ZipEntry(OUTBOUND));
                zipOutputStream.write(diff());
                zipOutputStream.closeEntry();

                zipOutputStream.putNextEntry(new ZipEntry(OUTBOUND_HASH));
                zipOutputStream.write(hashOf(outboundContents.get()));
                zipOutputStream.closeEntry();
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        return resultBuffer.toByteArray();
    }

    private static byte[] hashOf(byte[] data) {
        return Hashing.md5().hashBytes(data).asBytes();
    }


    private static ZipInputStream unzip(byte[] tarredContents) {
        return new ZipInputStream(new ByteArrayInputStream(tarredContents));
    }


    public byte[] getInboundContents() {
        return inboundContents;
    }

    public Optional<byte[]> getOutboundContents() {
        return outboundContents;
    }
}
