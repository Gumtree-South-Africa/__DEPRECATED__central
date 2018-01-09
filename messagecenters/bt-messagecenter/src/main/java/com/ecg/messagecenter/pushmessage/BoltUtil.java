package com.ecg.messagecenter.pushmessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.ecg.messagecenter.util.PropertyLookup;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * @author darasti
 *
 */
public class BoltUtil {

	private static final Logger logger = LoggerFactory.getLogger(BoltUtil.class);
	
	public static String getUserId(String userEmail) {
		if (userEmail == null || userEmail.isEmpty()) {
			return null;
		}
		UserSnapshot[] users;
        try {
        	String url = UriComponentsBuilder.fromHttpUrl(PropertyLookup.getuserSnapshotKernelUrl()).queryParam("emails", userEmail).build(false).encode().toUriString();
            logger.trace("Kernel URL: " + url);
            users = new RestTemplate().getForObject(url, UserSnapshot[].class);
        } catch (Exception e) {
            logger.error("Exception making Kernel API call for: " + userEmail, e);
        	return null;
        }
        if (users == null || users.length == 0 || users[0] == null || users[0].getUserId() == null) {
        	logger.error("empty user returned for: " + userEmail);
        	return null;
        }
        return users[0].getUserId();
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class UserSnapshot {
		private String userId;
		public String getUserId() {
			return userId;
		}
	}
}
