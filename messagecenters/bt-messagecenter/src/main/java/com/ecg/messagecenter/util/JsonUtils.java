package com.ecg.messagecenter.util;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
/**
 * Essential JSON helper utility methods.
 * @author nbarkhatov
 *
 */
public final class JsonUtils {

	static Logger logger = LoggerFactory.getLogger(JsonUtils.class);

	/**
	 * Based on the documentation, {@link ObjectMapper} is thread-safe and can be
	 * reused and shared globally.  However, it is found that in rare circumstance, it could
	 * run into a bad state and the instance can't be reused further.
	 *
	 * @see <a href="https://jira.corp.ebay.com/browse/BOLT-4779">BOLT-4779</a>
	 */
	static ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
    static {
    	mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    	mapper.registerModule(new JodaModule());
    }

	private JsonUtils() {}

	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			return mapper.readValue(json, clazz);
		} catch (IOException ex) {
			//
			// In rare circumstances, mapper could run into a corrupted state
			// where it can't recover by itself.  Need to create a new instance.
			//
			createTempMapperOnError();
			logger.error("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);
			throw new JsonRuntimeException("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);
		}
	}

    public static <T> T fromJsonIgnoreUnknownProperties(String json, Class<T> clazz) {
        try {
        	mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        	mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            return mapper.readValue(json, clazz);
        } catch (IOException ex) {
            //
            // In rare circumstances, mapper could run into a corrupted state
            // where it can't recover by itself.  Need to create a new instance.
            //
        	createTempMapperOnError();
            logger.error("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);
            throw new JsonRuntimeException("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);
        }
    }

	public static <T> T fromJson(String json, TypeReference<T> valueTypeRef) {
		try {
			return mapper.readValue(json, valueTypeRef);
		} catch (IOException ex) {
			//
			// In rare circumstances, mapper could run into a corrupted state
			// where it can't recover by itself.  Need to create a new instance.
			//
			createTempMapperOnError();
			logger.error("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);
			throw new JsonRuntimeException("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);
		}
	}

	public static JsonNode fromJson(String json) { 
		try { 
			return mapper.readTree(json);
		} catch(Exception ex) {
			//
			// In rare circumstances, mapper could run into a corrupted state
			// where it can't recover by itself.  Need to create a new instance.
			//
			createTempMapperOnError();
			logger.error("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);
			throw new JsonRuntimeException("FROM-JSON read exception, see nested...\nJSON:\n" + json + "\n", ex);			
		}
	}
	
	public static String toJson(Object jsonObject) {
		return toJson(jsonObject, false);
	}

	public static String toJsonPretty(Object jsonObject) {
		return toJson(jsonObject, true);
	}

	private static String toJson(Object jsonObject, boolean prettyPrinter) {
		try {
			mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
			return prettyPrinter ?
					mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject) :
					mapper.writeValueAsString(jsonObject);
		} catch (IOException ex) {
			//
			// In rare circumstances, mapper could run into a corrupted state
			// where it can't recover by itself.  Need to create a new instance.
			//
			createTempMapperOnError();
			logger.error("TO-JSON write exception, see nested...", ex);
			throw new JsonRuntimeException("TO-JSON write exception, see nested...", ex);
		}
	}

	
	private static void createTempMapperOnError() {
		
		ObjectMapper tmpMapper = new ObjectMapper();
		
		tmpMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		tmpMapper.registerModule(new JodaModule());
    	mapper = tmpMapper;
		
	}
}
