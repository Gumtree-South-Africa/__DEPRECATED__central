package com.ecg.replyts.core.webapi.util;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.junit.Assert.assertTrue;

public class JsonNodeMessageCoverterTest {

    private JsonNodeMessageConverter jnmc = new JsonNodeMessageConverter();
    private HttpHeaders headers;
    private HttpOutputMessage out;
    private ByteArrayOutputStream os;

    @Before
    public void setup() throws Exception {
        out = Mockito.mock(HttpOutputMessage.class);
        headers = Mockito.mock(HttpHeaders.class);
        Mockito.when(headers.getContentType()).thenReturn(MediaType.APPLICATION_JSON);
        Mockito.when(out.getHeaders()).thenReturn(headers);
        os = new ByteArrayOutputStream();
        Mockito.when(out.getBody()).thenReturn(os);
    }

    @Test
    public void supportsWritingJacksonObjects() {
        assertTrue(jnmc.canWrite(ObjectNode.class, MediaType.APPLICATION_JSON));
    }

    @Test
    public void convertsToJson() throws IOException {
        jnmc.write(JsonObjects.builder().attr("foo", "Bar").build(), MediaType.APPLICATION_JSON, out);

        String resp = new String(os.toByteArray());
        Assert.assertEquals("{\"foo\":\"Bar\"}", resp.replaceAll("[\\s\r\n]", ""));
    }

    @Test
    public void setsContentTypeCorrectly() throws Exception {
        jnmc.write(JsonObjects.newJsonObject(), MediaType.APPLICATION_JSON, out);

        Mockito.verify(headers).setContentType(new MediaType("application", "json", Charset.forName("UTF-8")));
    }

}
