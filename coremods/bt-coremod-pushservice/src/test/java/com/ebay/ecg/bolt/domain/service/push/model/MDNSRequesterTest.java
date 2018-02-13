package com.ebay.ecg.bolt.domain.service.push.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MDNSRequesterTest {
	private static final Locale LOCALE = new Locale("en","ZA");
	
	private static final String DEVICE_TOKEN = "token1";
	
	private static final String NOTIFICATION_TITLE = "You got a message!";

	@Test
	public void testSearchAlertPushMessages() throws Exception {
		Map<String, String> details = new HashMap<>();

		details.put("locale", LOCALE.toString());
		details.put("receiverId", "100000500");
		details.put("alertId", "100000900");

		PushMessagePayload pushMessagePayload = new PushMessagePayload("kemo@ebay.com", "Hello", "SEARCHALERTS", details);
		MDNSRequester mdnsRequester = new MDNSRequester(new PushHostInfo("123", "123", "123"));
    	HttpPost httpPost = mdnsRequester.build(pushMessagePayload, DEVICE_TOKEN, NOTIFICATION_TITLE);

    	Assert.assertNotNull(EntityUtils.toString(httpPost.getEntity()));
	}
}