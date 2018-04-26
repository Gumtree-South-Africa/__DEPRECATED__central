package com.ecg.messagecenter.ebayk.pushmessage;

import com.ecg.messagecenter.ebayk.pushmessage.PushMessagePayload;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Before;
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

    private Map<String, String> details = Maps.newHashMap();

    @Before
    public void setUp() throws Exception {
        details.put("a1","1");
    }

    @Test
    public void defaultNotIncludeAlertCounter() {

        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", details);

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails().get("a1"), "1");
        assertEquals(payload.getAlertCounter(), Optional.empty());
        assertEquals(payload.getGcmDetails(), Optional.empty());
        assertEquals(payload.getApnsDetails(), Optional.empty());
        assertEquals(payload.getDetails().get("utm_source"), "System");
        assertEquals(payload.getDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getDetails().containsKey("utm_content"));

        System.out.println(payload.asJson());
        assertTrue(payload.asJson().toString().contains("{\"email\":\"a\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\",\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":"));
    }

    @Test
    public void includeAlertCounter() {
        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", details, Optional.of(10), Optional.<Map<String, String>>empty(), Optional.<Map<String, String>>empty());

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails().get("a1"), "1");
        assertEquals(payload.getAlertCounter(), Optional.of(10));
        assertEquals(payload.getGcmDetails(), Optional.empty());
        assertEquals(payload.getApnsDetails(), Optional.empty());
        assertEquals(payload.getDetails().get("utm_source"), "System");
        assertEquals(payload.getDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getDetails().containsKey("utm_content"));
        assertTrue((payload.asJson().get("email").toString().contains("a")));
        assertTrue((payload.asJson().get("message").toString().contains("b")));
        assertTrue((payload.asJson().get("activity").toString().contains("c")));
        assertTrue((payload.asJson().get("details").toString().contains("{\"a1\":\"1\",\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":\"")));
        assertTrue((payload.asJson().get("alertCounter").toString().contains("10")));
    }

    @Test
    public void includePushServiceSpecificGcmParams() {
        Map<String, String> gcmDetails = new LinkedHashMap<String, String>();
        gcmDetails.put("gcmSpecific1", "value1");
        gcmDetails.put("gcmSpecific2", "value2");

        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", details, Optional.of(10), Optional.of(gcmDetails), Optional.<Map<String, String>>empty());

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails().get("a1"), "1");
        assertEquals(payload.getAlertCounter(), Optional.of(10));
        assertEquals(payload.getGcmDetails(), Optional.of(ImmutableMap.of("gcmSpecific1", "value1", "gcmSpecific2", "value2")));
        assertEquals(payload.getDetails().get("utm_source"), "System");
        assertEquals(payload.getDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getDetails().containsKey("utm_content"));
        assertTrue((payload.asJson().get("email").toString().contains("a")));
        assertTrue((payload.asJson().get("message").toString().contains("b")));
        assertTrue((payload.asJson().get("activity").toString().contains("c")));
        System.out.println(payload.asJson().get("details"));
        assertTrue((payload.asJson().get("details").toString().contains("{\"a1\":\"1\",\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":\"")));
        assertTrue((payload.asJson().get("alertCounter").toString().contains("10")));
        assertTrue((payload.asJson().get("gcmDetails").toString().contains("{\"gcmSpecific1\":\"value1\",\"gcmSpecific2\":\"value2\"}")));
    }

    @Test
    public void includePushServiceSpecificApnsParams() {
        Map<String, String> apnsDetails = new LinkedHashMap<String, String>();
        apnsDetails.put("apnsSpecific1", "value1");
        apnsDetails.put("apnsSpecific2", "value2");

        PushMessagePayload payload = new PushMessagePayload("a", "b", "c", details, Optional.of(10), Optional.<Map<String, String>>empty(), Optional.of(apnsDetails));

        assertEquals(payload.getEmail(), "a");
        assertEquals(payload.getMessage(), "b");
        assertEquals(payload.getActivity(), "c");
        assertEquals(payload.getDetails().get("a1"), "1");
        assertEquals(payload.getAlertCounter(), Optional.of(10));
        assertEquals(payload.getApnsDetails(), Optional.of(ImmutableMap.of("apnsSpecific1", "value1", "apnsSpecific2", "value2")));
        assertEquals(payload.getDetails().get("utm_source"), "System");
        assertEquals(payload.getDetails().get("utm_medium"), "PushNotification");
        assertEquals(payload.getDetails().get("utm_campaign"), "NewMessage");
        assertTrue(payload.getDetails().containsKey("utm_content"));
        assertTrue((payload.asJson().get("email").toString().contains("a")));
        assertTrue((payload.asJson().get("message").toString().contains("b")));
        assertTrue((payload.asJson().get("activity").toString().contains("c")));
        assertTrue((payload.asJson().get("details").toString().contains("{\"a1\":\"1\",\"utm_source\":\"System\",\"utm_medium\":\"PushNotification\",\"utm_campaign\":\"NewMessage\",\"utm_content\":\"")));
        assertTrue((payload.asJson().get("alertCounter").toString().contains("10")));
        assertTrue((payload.asJson().get("apnsDetails").toString().contains("{\"apnsSpecific1\":\"value1\",\"apnsSpecific2\":\"value2\"}")));
    }

}
