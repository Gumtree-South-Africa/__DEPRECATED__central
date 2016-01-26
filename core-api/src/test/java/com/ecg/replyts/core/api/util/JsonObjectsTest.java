package com.ecg.replyts.core.api.util;

import org.junit.Test;

import java.util.Arrays;

import static com.ecg.replyts.core.api.util.JsonObjects.builder;
import static com.ecg.replyts.core.api.util.JsonObjects.newJsonArray;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class JsonObjectsTest {

    @Test
    public void buildsNewObject() {
        String json = builder().attr("foo", "bar").toJson();
        assertEquals("{\"foo\":\"bar\"}", json);
    }

    @Test
    public void nestsBuilders() {
        assertEquals("{\"x\":{\"y\":\"z\"}}", builder().attr("x", builder().attr("y", "z")).toJson());
    }

    @Test
    public void doesNotQuoteNumerics() {

        String json = builder().attr("foo", 1).toJson();
        assertEquals("{\"foo\":1}", json);
    }

    @Test
    public void doesNotQuoteBooleans() {

        String json = builder().attr("foo", false).toJson();
        assertEquals("{\"foo\":false}", json);
    }

    @Test
    public void nestsJsonArrays() {
        String json = builder().attr("foo", newJsonArray(asList("a", "b"))).toJson();
        assertEquals("{\"foo\":[\"a\",\"b\"]}", json);

    }


    @Test
    public void parsesJsonObjects() {
        assertEquals(1, JsonObjects.parse("{\"foo\":1}").get("foo").asInt());

    }


    @Test
    public void handlesSingleQuotesInParsing() {
        assertEquals(1, JsonObjects.parse("{'foo':1}").get("foo").asInt());
    }

    @Test
    public void handlesMissingKeyQuotes() {
        assertEquals(1, JsonObjects.parse("{foo:1}").get("foo").asInt());
    }
}
