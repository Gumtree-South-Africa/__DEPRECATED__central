package com.ecg.de.ebayk.messagecenter.pushmessage;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class PushMessagePayloadTest {


    @Test public void defaultNotIncludeAlertCounter() {
        PushMessagePayload payload =
                        new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"));

        assertEquals("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"}}",
                        payload.asJson());
    }


    @Test public void includeAlertCounter() {
        PushMessagePayload payload =
                        new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"),
                                        Optional.of(10), Optional.<Map<String, String>>absent(),
                                        Optional.<Map<String, String>>absent(),
                                        Optional.<Map<String, String>>absent());

        assertEquals("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10}",
                        payload.asJson());
    }

    @Test public void includePushServiceSpecificGcmParams() {
        Map<String, String> gcmDetails = new LinkedHashMap<String, String>();
        gcmDetails.put("gcmSpecific1", "value1");
        gcmDetails.put("gcmSpecific2", "value2");

        PushMessagePayload payload =
                        new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"),
                                        Optional.of(10), Optional.of(gcmDetails),
                                        Optional.<Map<String, String>>absent(),
                                        Optional.<Map<String, String>>absent());

        assertEquals("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10,\"gcmDetails\":{\"gcmSpecific1\":\"value1\",\"gcmSpecific2\":\"value2\"}}",
                        payload.asJson());
    }

    @Test public void includePushServiceSpecificApnsParams() {
        Map<String, String> apnsDetails = new LinkedHashMap<String, String>();
        apnsDetails.put("apnsSpecific1", "value1");
        apnsDetails.put("apnsSpecific2", "value2");

        PushMessagePayload payload =
                        new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"),
                                        Optional.of(10), Optional.<Map<String, String>>absent(),
                                        Optional.of(apnsDetails),
                                        Optional.<Map<String, String>>absent());

        assertEquals("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10,\"apnsDetails\":{\"apnsSpecific1\":\"value1\",\"apnsSpecific2\":\"value2\"}}",
                        payload.asJson());
    }

    @Test public void includePushServiceSpecificNotificationsParams() {
        Map<String, String> notificationsDetails = new LinkedHashMap<String, String>();
        notificationsDetails.put("apnsSpecific1", "value1");
        notificationsDetails.put("apnsSpecific2", "value2");

        PushMessagePayload payload =
                        new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"),
                                        Optional.of(10), Optional.<Map<String, String>>absent(),
                                        Optional.<Map<String, String>>absent(),
                                        Optional.of(notificationsDetails));

        assertEquals("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10,\"notificationsDetails\":{\"apnsSpecific1\":\"value1\",\"apnsSpecific2\":\"value2\"}}",
                        payload.asJson());
    }



}
