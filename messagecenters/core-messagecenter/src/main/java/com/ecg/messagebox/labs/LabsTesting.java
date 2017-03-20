package com.ecg.messagebox.labs;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LabsTesting {

    public static boolean matches(String userId, int[] rangeBounds) {
        HashCode hashCode = Hashing.murmur3_32().hashString(userId, Charsets.UTF_8);
        int percentage = Math.abs(hashCode.asInt()) % 100;
        IntStream range = IntStream.range(rangeBounds[0], rangeBounds[1]);
        return range.anyMatch(i -> i == percentage);
    }

    public static int[] getRange(String rangeStr) {
        return Stream.of(rangeStr.trim().split("-"))
                .mapToInt(Integer::parseInt)
                .toArray();
    }

    public static List<String> getUserIds(String userIdsStr) {
        return Arrays.asList(userIdsStr.split(",")).stream()
                .map(String::trim)
                .filter(userId -> userId.length() > 0)
                .collect(Collectors.toList());
    }
}