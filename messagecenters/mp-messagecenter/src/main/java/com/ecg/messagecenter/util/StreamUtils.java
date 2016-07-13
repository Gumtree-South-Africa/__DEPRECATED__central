package com.ecg.messagecenter.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {

    /**
     * Converts an iterable to a stream without any parallelism.
     *
     * @param iterable iterable
     * @param <T> iterable element type
     * @return a stream
     */
    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }
}
