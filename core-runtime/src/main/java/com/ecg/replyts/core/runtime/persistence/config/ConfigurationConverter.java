package com.ecg.replyts.core.runtime.persistence.config;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.Converter;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.runtime.persistence.GzipAwareContentFilter;
import com.ecg.replyts.core.runtime.persistence.ValueSizeConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

import static java.lang.String.format;

class ConfigurationConverter implements Converter<Configurations> {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationConverter.class);

    public static final String KEY = "config";

    // Because we manage the regex of our filter, the configuration is bigger than recommended for Riak (~4 MB).
    // Therefore we provide as alternative a compressed version of the configuration.
    private static final GzipAwareContentFilter CONTENT_FILTER = new GzipAwareContentFilter(StandardCharsets.UTF_8);

    private final ValueSizeConstraint sizeConstraint;

    private final String configBucketName;

    private final ConfigurationJsonSerializer serializer = new ConfigurationJsonSerializer();

    ConfigurationConverter(String configBucketName) {
        this(ValueSizeConstraint.maxMb(30), configBucketName);
    }

    ConfigurationConverter(ValueSizeConstraint constraint, String configBucketName) {
        this.sizeConstraint = constraint;
        this.configBucketName = configBucketName;
    }

    @Override
    public IRiakObject fromDomain(Configurations domainObject, VClock vclock) {
        String json = serializer.fromDomain(domainObject);
        JsonObjects.parse(json);

        int configurationSizeBytes = json.getBytes().length;

        if (sizeConstraint.isTooBig(configurationSizeBytes)) {
            throw new IllegalArgumentException(format("Rejecting to store Configuration. Size is too big: %s bytes", configurationSizeBytes));
        }

        RiakObjectBuilder builder = RiakObjectBuilder
                .newBuilder(configBucketName, KEY)
                .withVClock(vclock);

        CONTENT_FILTER.writeStringToRiakObject(builder, json, domainObject.isCompressed());

        return builder.build();
    }

    @Override
    public Configurations toDomain(IRiakObject riakObject) {
        if (riakObject != null) {
            String configValueString = CONTENT_FILTER.readStringFromRiakObject(riakObject);
            boolean compressed = CONTENT_FILTER.isCompressed(riakObject);
            return serializer.toDomain(configValueString).setCompressed(compressed);

        } else {
            LOG.warn("Could not find key {} in configuration bucket: no filter configurations found.", KEY);
            return Configurations.EMPTY_CONFIG_SET;
        }
    }

}
