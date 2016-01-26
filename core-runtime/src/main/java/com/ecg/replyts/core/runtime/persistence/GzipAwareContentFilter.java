package com.ecg.replyts.core.runtime.persistence;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.http.util.Constants;

import java.nio.charset.Charset;

/**
 * Write an read the content from an Riak object.
 * <p/>
 * Depending on the content type can compress/uncompress the content.
 */
public final class GzipAwareContentFilter {

    private final Charset charset;

    /**
     * @param charset The charset to use for encoding the compressed string
     */
    public GzipAwareContentFilter(Charset charset) {
        this.charset = charset;
    }

    public String readStringFromRiakObject(IRiakObject object) {
        if (isCompressed(object)) {
            byte[] bytes = GZip.unzip(object.getValue());
            return new String(bytes, charset);
        }
        // relay on the charset stored in the bucket
        return object.getValueAsString();
    }

    /**
     * Writes the content to the Riak object. Depending on if the content have to compress, zip the
     * content and sets the correct content type.
     * In the case of zip, content type {@code application/x-gzip} otherwise {@code application/json; charset=UTF-8}
     *
     * @param builder    The riak object builder where to store the content
     * @param content    The content as string
     * @param compressed If the content should be compressed
     */
    public void writeStringToRiakObject(RiakObjectBuilder builder, String content, boolean compressed) {
        if (compressed) {
            byte[] bytes = GZip.zip(content.getBytes(charset));
            builder.withValue(bytes)
                    .withContentType(GZip.GZIP_MIMETYPE);
        } else {
            builder.withValue(content)
                    .withContentType(Constants.CTYPE_JSON_UTF8);
        }
    }

    /**
     * @return True, if the content of the riak object are compressed.
     */
    public boolean isCompressed(IRiakObject object) {
        return GZip.GZIP_MIMETYPE.equals(object.getContentType());
    }
}
