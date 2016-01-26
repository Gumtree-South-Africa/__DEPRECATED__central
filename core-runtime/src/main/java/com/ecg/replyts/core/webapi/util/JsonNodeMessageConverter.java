package com.ecg.replyts.core.webapi.util;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collections;

/**
 * Converts incoming json data into a Jackson {@link JsonNode} and converts {@link ResponseBody} objects of
 * {@link JsonNode} type into Json. Ensures the content type and charset are set correctly to
 * <code>application/json; charset=utf-8</code>
 *
 * @author mhuttar
 */
public class JsonNodeMessageConverter extends AbstractHttpMessageConverter<JsonNode> {

    private static final MediaType APP_JSON_UTF8 = new MediaType("application", "json", Charset.forName("UTF-8"));

    private final ObjectMapper om = JsonObjects.newPrettyPrintObjectMapper();

    public JsonNodeMessageConverter() {
        super();

        setSupportedMediaTypes(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return JsonNode.class.isAssignableFrom(clazz);
    }

    @Override
    protected JsonNode readInternal(Class<? extends JsonNode> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        return om.readTree(inputMessage.getBody());
    }

    @Override
    protected void writeInternal(JsonNode t, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        outputMessage.getHeaders().setContentType(APP_JSON_UTF8);

        Writer outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), "UTF-8");
        om.writeValue(outputStreamWriter, t);
    }


}
