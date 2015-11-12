package nl.marktplaats.replyts2.util;

import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;

import java.util.List;

public final class MediaTypeHelper {

    private static final List<MediaType> HTML_COMPATIBLE_TYPES = ImmutableList.of(
            MediaType.create("text", "html"),
            MediaType.create("text", "xml"),
            MediaType.create("application", "xhtml+xml"),
            MediaType.create("application", "xml"));

    private MediaTypeHelper() {
    }

    private static boolean areMediaTypesEqual(MediaType mediaType, MediaType otherMediaType) {
        return mediaType.type().equals(otherMediaType.type()) && mediaType.subtype().equals(otherMediaType.subtype());
    }

    public static boolean isHtmlCompatibleType(MediaType mediaType) {
        return HTML_COMPATIBLE_TYPES.stream().anyMatch(mt -> areMediaTypesEqual(mt, mediaType));
    }

    public static boolean isPlainTextCompatible(MediaType mediaType) {
        return mediaType.is(MediaType.ANY_TEXT_TYPE);
    }
}
