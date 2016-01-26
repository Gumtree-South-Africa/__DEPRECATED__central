package com.ecg.replyts.core.runtime.persistence.conversation;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.IOException;

/**
 * Changes {@link JodaModule} to use the local date time zone for
 * deserialization.
 */
class JodaModuleWithLocalDateTimeZone extends JodaModule {

    public JodaModuleWithLocalDateTimeZone() {
        super();
        addDeserializer(DateTime.class, new CustomDateTimeDeserializer());
    }

    private static class CustomDateTimeDeserializer extends JsonDeserializer<DateTime> {
        private JsonDeserializer<DateTime> wrappedDeserializer = DateTimeDeserializer.forType(DateTime.class);

        @Override
        public DateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            DateTime wrappedResult = wrappedDeserializer.deserialize(jsonParser, deserializationContext);
            if (wrappedResult == null) return null;
            return wrappedResult.withZone(DateTimeZone.getDefault());
        }
    }

}
