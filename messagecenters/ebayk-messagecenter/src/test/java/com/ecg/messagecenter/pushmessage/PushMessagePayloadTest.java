package com.ecg.messagecenter.pushmessage;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class PushMessagePayloadTest {


    @Test
    public void defaultNotIncludeAlertCounter() {
        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"));

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails(), ImmutableMap.of("a1", "1"));
        assertEquals(payload.getAlertCounter(), Optional.empty());
        assertEquals(payload.getGcmDetails(), Optional.empty());
        assertEquals(payload.getApnsDetails(), Optional.empty());
        assertEquals(payload.getUtmDetails().get("utm_source"), "System");
        assertEquals(payload.getUtmDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getUtmDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getUtmDetails().containsKey("utm_content"));

        assertTrue(payload.asJson().contains("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"utmDetails\":{\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":"));
    }

    @Test
    public void includeAlertCounter() {
        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"), Optional.of(10), Optional.<Map<String, String>>empty(), Optional.<Map<String, String>>empty());

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails(), ImmutableMap.of("a1", "1"));
        assertEquals(payload.getAlertCounter(), Optional.of(10));
        assertEquals(payload.getGcmDetails(), Optional.empty());
        assertEquals(payload.getApnsDetails(), Optional.empty());
        assertEquals(payload.getUtmDetails().get("utm_source"), "System");
        assertEquals(payload.getUtmDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getUtmDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getUtmDetails().containsKey("utm_content"));

        assertTrue(payload.asJson().contains("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10,\"utmDetails\":{\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":"));
    }

    @Test
    public void includePushServiceSpecificGcmParams() {
        Map<String, String> gcmDetails = new LinkedHashMap<String, String>();
        gcmDetails.put("gcmSpecific1", "value1");
        gcmDetails.put("gcmSpecific2", "value2");

        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"), Optional.of(10), Optional.of(gcmDetails), Optional.<Map<String, String>>empty());

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails(), ImmutableMap.of("a1", "1"));
        assertEquals(payload.getAlertCounter(), Optional.of(10));
        assertEquals(payload.getGcmDetails(), Optional.of(ImmutableMap.of("gcmSpecific1", "value1", "gcmSpecific2", "value2")));
        assertEquals(payload.getUtmDetails().get("utm_source"), "System");
        assertEquals(payload.getUtmDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getUtmDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getUtmDetails().containsKey("utm_content"));
        assertTrue(payload.asJson().contains("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10,\"gcmDetails\":{\"gcmSpecific1\":\"value1\",\"gcmSpecific2\":\"value2\"},\"utmDetails\":{\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":"));
    }

    @Test
    public void includePushServiceSpecificApnsParams() {
        Map<String, String> apnsDetails = new LinkedHashMap<String, String>();
        apnsDetails.put("apnsSpecific1", "value1");
        apnsDetails.put("apnsSpecific2", "value2");

        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", ImmutableMap.of("a1", "1"), Optional.of(10), Optional.<Map<String, String>>empty(), Optional.of(apnsDetails));

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails(), ImmutableMap.of("a1", "1"));
        assertEquals(payload.getAlertCounter(), Optional.of(10));
        assertEquals(payload.getApnsDetails(), Optional.of(ImmutableMap.of("apnsSpecific1", "value1", "apnsSpecific2", "value2")));
        assertEquals(payload.getUtmDetails().get("utm_source"), "System");
        assertEquals(payload.getUtmDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getUtmDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getUtmDetails().containsKey("utm_content"));
        assertTrue(payload.asJson().contains("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10,\"apnsDetails\":{\"apnsSpecific1\":\"value1\",\"apnsSpecific2\":\"value2\"},\"utmDetails\":{\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":"));
    }


}
