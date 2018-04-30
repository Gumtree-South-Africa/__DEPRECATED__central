package com.ecg.messagecenter.core.persistence.simple;

import com.basho.riak.client.IRiakObject;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import java.util.Objects;

public final class RiakGzipAwareContentFilter {
    public static final String GZIP_MIMETYPE = "application/x-gzip";

    public static String unpackIfGzipped(IRiakObject object) {
        if (Objects.equals(object.getContentType(), GZIP_MIMETYPE)) {

            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(object.getValue()));) {

                return new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return object.getValueAsString();
    }

    public static byte[] compress(String payload) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try(GZIPOutputStream  gzipOutputStream = new GZIPOutputStream(os);) {

            gzipOutputStream.write(payload.getBytes(Charsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return os.toByteArray();
    }
}
