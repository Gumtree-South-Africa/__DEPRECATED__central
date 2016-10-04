package com.ecg.replyts.integration.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Random;

/**
 * 'Sends' a mail by putting it in the Reply T&S drop folder.
 */
public class FileSystemMailSender {


    private final File dropFolder;
    private final Random random = new Random();

    public FileSystemMailSender(File dropFolder) {
        this.dropFolder = dropFolder;
    }

    public void sendMail(byte[] data) {
        int bufferSize = 40000;
        int fileId = 100000 + random.nextInt(899999);

        File dropFile = new File(dropFolder, "drop_" + fileId);
        File pickupFile = new File(dropFolder, "pre_" + fileId);

        try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(dropFile), bufferSize)) {
            stream.write(data);
            stream.close();
            if (!dropFile.renameTo(pickupFile)) {
                throw new RuntimeException(String.format("rename form %s to %s failed", dropFile.toString(), pickupFile.toString()));
            }

        } catch (Exception e) {
            dropFile.delete();
            pickupFile.delete();
            throw new RuntimeException(e);
        }
    }

}
