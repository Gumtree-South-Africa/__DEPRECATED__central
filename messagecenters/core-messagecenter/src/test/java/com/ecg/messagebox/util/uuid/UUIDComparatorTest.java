package com.ecg.messagebox.util.uuid;

import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class UUIDComparatorTest {

    private static final UUID TIME_UUID_1 = UUID.fromString("65fe5390-01ba-11e6-9f14-0940c3c3cb28"); // 2016-04-13 20:57:56+0000
    private static final UUID TIME_UUID_2 = UUID.fromString("a24df080-1e07-11e6-b4a1-c9bc3263ebd1"); // 2016-05-19 21:21:21+0000
    private static final UUID TIME_UUID_3 = UUID.fromString("f51a9230-5ffe-11e6-b6b6-ad6ad91c5d69"); // 2016-08-11 20:05:31+0000
    private static final UUID TIME_UUID_4 = UUID.fromString("f7320080-5ffe-11e6-b20d-af02d5e06288"); // 2016-08-11 20:05:35+0000

    private static final List<UUID> TIME_UUIDS = newArrayList(TIME_UUID_3, TIME_UUID_1, TIME_UUID_2, TIME_UUID_4);

    @Test
    public void sortTimeUUIDsAscending() {
        List<UUID> expected = newArrayList(TIME_UUID_1, TIME_UUID_2, TIME_UUID_3, TIME_UUID_4);

        List<UUID> actual = TIME_UUIDS.stream()
                .sorted(UUIDComparator::staticCompare)
                .collect(Collectors.toList());

        assertThat(actual, is(expected));
    }

    @Test
    public void sortTimeUUIDsDescending() {
        List<UUID> expected = newArrayList(TIME_UUID_4, TIME_UUID_3, TIME_UUID_2, TIME_UUID_1);

        List<UUID> actual = TIME_UUIDS.stream()
                .sorted((u1, u2) -> UUIDComparator.staticCompare(u2, u1))
                .collect(Collectors.toList());

        assertThat(actual, is(expected));
    }
}