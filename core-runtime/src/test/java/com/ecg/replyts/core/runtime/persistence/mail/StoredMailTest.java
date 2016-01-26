package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.runtime.persistence.GZip;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import org.apache.commons.codec.binary.Hex;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class StoredMailTest {

    @Test
    public void createsCompressedArchive() {
        new StoredMail("these are inbound mail contents!".getBytes(), Optional.of("these are outbound mail contents".getBytes())).compress();
    }

    @Test
    public void createsCompresedArchiveWithOnlyInboundMail() {
        new StoredMail("these are inbound mail contents!".getBytes(), Optional.<byte[]>absent()).compress();
    }

    @Test
    public void extractsCompressedArchive() {
        byte[] result = new StoredMail("these are inbound mail contents!".getBytes(), Optional.of("these are outbound mail contents".getBytes())).compress();
        StoredMail reloaded = StoredMail.extract(result);

        assertEquals("these are inbound mail contents!", new String(reloaded.getInboundContents()));
        assertEquals("these are outbound mail contents", new String(reloaded.getOutboundContents().get()));
    }

    @Test
    @Ignore
    public void runAgainstShellBenchmark() throws IOException, InterruptedException {
        System.out.println("ORIG      \tCURR      \tNEW       \tNEW > CURR");

        for (File file : new File("/tmp/mails").listFiles()) {
            if (file.getName().endsWith(".in")) {
                printCompressionStats(file, new File(file.getParent(), file.getName().substring(0, file.getName().length() - 3) + ".out"));
            }
        }

        System.out.printf("%10d\t%10d\t%10d\n", totalUncompressed, totalCurrent, totalNew);
    }

    long totalUncompressed = 0, totalCurrent = 0, totalNew = 0;

    @Test
    @Ignore
    public void compressOneFile() {
        printCompressionStats(new File("/tmp/mail-in"), new File("/tmp/mail-out"));
    }

    private void printCompressionStats(File inF, File outF) {
        try {
            byte[] in = Files.toByteArray(inF);
            byte[] out = Files.toByteArray(outF);

            String outOrig;
            try {
                outOrig = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(out));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e.getMessage());
            }

            byte[] compressed = new StoredMail(in, Optional.of(out)).compress();
            int sizeBytes = compressed.length;


            totalUncompressed += in.length + out.length;
            int currentCompression = (compressed(in) + compressed(out));
            int newCompression = sizeBytes;
            totalCurrent += currentCompression;
            totalNew += newCompression;
            System.out.printf("%10d\t%10d\t%10d\n", (in.length + out.length), currentCompression, newCompression);

            String outFinal;
            try {
                outFinal = Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(StoredMail.extract(compressed).getOutboundContents().get()));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e.getMessage());
            }

            assertEquals(outOrig, outFinal);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int compressed(byte[] in) {
        return GZip.zip(in).length;
    }
}
