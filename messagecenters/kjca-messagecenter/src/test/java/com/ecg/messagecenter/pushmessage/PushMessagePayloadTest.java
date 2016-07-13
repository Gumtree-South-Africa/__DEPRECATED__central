package com.ecg.messagecenter.pushmessage;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;


/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class PushMessagePayloadTest {


    @Test
    public void includeAlertCounter() {
        PushMessagePayload payload = new PushMessagePayload("a", "1234", "b", "c", Optional.of(ImmutableMap.of("a1", "1")), Optional.of(10));

        assertEquals("{\"email\":\"a\",\"userId\":\"1234\",\"message\":\"b\",\"activity\":\"c\",\"details\":{\"a1\":\"1\"},\"alertCounter\":10}", payload.asJson());
    }

}
