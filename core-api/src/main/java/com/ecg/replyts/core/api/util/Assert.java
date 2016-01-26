package com.ecg.replyts.core.api.util;

public class Assert {

    public static void notNull(Object value) {
        if (value == null) throw new IllegalArgumentException("must not be null");
    }

}
