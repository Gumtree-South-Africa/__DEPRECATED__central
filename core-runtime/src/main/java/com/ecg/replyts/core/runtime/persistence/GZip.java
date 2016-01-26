package com.ecg.replyts.core.runtime.persistence;

import com.google.common.io.ByteStreams;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Wrap some boilerplate code for gzip content.
 */
public final class GZip {

    public static final String GZIP_MIMETYPE = "application/x-gzip";

    public static byte[] unzip(byte[] payload) {
        try (InputStream gzipped = new GZIPInputStream(new ByteArrayInputStream(payload))) {
            return ByteStreams.toByteArray(gzipped);
        } catch (IOException e) {
            throw new RuntimeException("could not unzip content", e);
        }
    }

    public static byte[] zip(byte[] payload) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (OutputStream gzipOutputStream = new GZIPOutputStream(out)) {
            gzipOutputStream.write(payload);
        } catch (IOException e) {
            throw new RuntimeException("could not zip content", e);
        }

        return out.toByteArray();
    }

}
