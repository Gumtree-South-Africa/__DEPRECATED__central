package com.ecg.comaas.mp.postprocessor.anonymizebody;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;

import java.util.List;

/**
 * Created by reweber on 08/10/15
 */
public final class MediaTypeHelper {

    private MediaTypeHelper() {}

    private static final List<MediaType> HTML_COMPATIBLE_TYPES = ImmutableList.of(
            MediaType.create("text", "html"),
            MediaType.create("text", "xml"),
            MediaType.create("application", "xhtml+xml"),
            MediaType.create("application", "xml"));

    static boolean isHtmlCompatibleType(MediaType mediaType) {
        return HTML_COMPATIBLE_TYPES.stream().anyMatch(mt -> areMediaTypesEqual(mt, mediaType));
    }

    private static boolean areMediaTypesEqual(MediaType mediaType, MediaType otherMediaType) {
        return mediaType.type().equals(otherMediaType.type()) && mediaType.subtype().equals(otherMediaType.subtype());
    }
}
