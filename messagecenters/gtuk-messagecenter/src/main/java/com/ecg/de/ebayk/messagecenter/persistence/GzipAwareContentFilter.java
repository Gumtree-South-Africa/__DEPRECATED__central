package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.IRiakObject;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

final class GzipAwareContentFilter {

    public static final String GZIP_MIMETYPE = "application/x-gzip";

    private GzipAwareContentFilter() {
    }

    public static String unpackIfGzipped(IRiakObject object) {
        if (Objects.equal(object.getContentType(), GZIP_MIMETYPE)) {

            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(object.getValue());
            final GZIPInputStream in;
            try {
                in = new GZIPInputStream(byteArrayInputStream);
                return new String(ByteStreams.toByteArray(in), Charsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(byteArrayInputStream);
            }
        }
        return object.getValueAsString();
    }

    public static byte[] compress(String payload) {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream;
        try {
            gzipOutputStream = new GZIPOutputStream(os);
            gzipOutputStream.write(payload.getBytes(Charsets.UTF_8));
            gzipOutputStream.finish();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(os);
        }

    }
}
