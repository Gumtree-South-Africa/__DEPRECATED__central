package com.ecg.messagecenter.kjca.pushmessage.send.model;

import com.ecg.messagecenter.kjca.pushmessage.send.client.SendClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Subscription {
    private static final Logger LOG = LoggerFactory.getLogger(Subscription.class);

    @JsonProperty("id")
    private Long id;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("deviceToken")
    private String deviceToken;

    @JsonProperty("deliveryService")
    private String deliveryService;

    @JsonProperty("type")
    private SendClient.NotificationType type;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("locale")
    @JsonDeserialize(using = LocaleDeserializer.class)
    private Locale locale;

    @JsonProperty("creation-date")
    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeserializer.class)
    private ZonedDateTime creationDate;

    @JsonProperty("modification-date")
    @JsonSerialize(using = DateTimeSerializer.class)
    @JsonDeserialize(using = DateTimeDeserializer.class)
    private ZonedDateTime modificationDate;

    private Subscription() {
        this(null, null, null, null, null, false, null, null, null);
    }

    private Subscription(Long id,
                         Long userId,
                         String deviceToken,
                         String deliveryService,
                         SendClient.NotificationType type,
                         boolean enabled,
                         Locale locale,
                         ZonedDateTime creationDate,
                         ZonedDateTime modificationDate) {
        this.id = id;
        this.userId = userId;
        this.deviceToken = deviceToken;
        this.deliveryService = deliveryService;
        this.type = type;
        this.enabled = enabled;
        this.locale = locale;
        this.creationDate = creationDate;
        this.modificationDate = modificationDate;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getDeliveryService() {
        return deliveryService;
    }

    public SendClient.NotificationType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Locale getLocale() {
        return locale;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public ZonedDateTime getModificationDate() {
        return modificationDate;
    }

    private static final class DateTimeSerializer extends JsonSerializer<ZonedDateTime> {
        @Override
        public void serialize(ZonedDateTime dateTime, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            if (dateTime != null) {
                jsonGenerator.writeString(dateTime.format(ISO_OFFSET_DATE_TIME));
            }
        }
    }

    private static final class DateTimeDeserializer extends JsonDeserializer<ZonedDateTime> {
        @Override
        public ZonedDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            return ZonedDateTime.parse(jsonParser.getValueAsString());
        }
    }

    private static final class LocaleDeserializer extends JsonDeserializer<Locale> {
        @Override
        public Locale deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            try {
                if ("fr_CA".equals(jsonParser.getValueAsString())) {
                    return Locale.CANADA_FRENCH;
                }
            } catch (IOException e) {
                LOG.error("Encountered " + e.getClass().getSimpleName() + " while attempting to parse a Locale as a string. Falling back on Canadian English.", e);
            }

            return Locale.CANADA;
        }
    }

}
