package com.ecg.replyts.core.runtime.persistence.conversation;

import com.basho.riak.client.IRiakObject;
import com.ecg.replyts.core.runtime.persistence.GZip;

public final class GzipAwareContentFilter {

    private GzipAwareContentFilter() {
    }

    public static byte[] unpackIfGzipped(IRiakObject object) {

        byte[] bytes = object.getValue();
        if (GZip.GZIP_MIMETYPE.equals(object.getContentType())) {
            return GZip.unzip(bytes);
        }
        return bytes;
    }

}
