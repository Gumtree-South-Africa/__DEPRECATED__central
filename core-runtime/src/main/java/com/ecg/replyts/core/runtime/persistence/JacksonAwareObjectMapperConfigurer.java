package com.ecg.replyts.core.runtime.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * Created by pragone
 * Created on 18/10/15 at 2:26 PM
 *
 * @author Paolo Ragone <pragone@ebay.com>
 */
public class JacksonAwareObjectMapperConfigurer {

    private final ObjectMapper objectMapper;

    public JacksonAwareObjectMapperConfigurer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JodaModule());
        this.objectMapper.registerModule(new GuavaModule());
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
