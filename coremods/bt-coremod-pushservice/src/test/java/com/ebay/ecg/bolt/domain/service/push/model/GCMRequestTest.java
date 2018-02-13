package com.ebay.ecg.bolt.domain.service.push.model;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GCMRequestTest {
	private static final Locale LOCALE = new Locale("en","ZA");
	
	private static final String DEVICE_TOKEN = "token1";
	
	private static final String NOTIFICATION_TITLE = "You got a message!";
	
	@Test
	public void testSendPushMessages() throws Exception {
		Map<String, String> details = Collections.singletonMap("locale", LOCALE.toString());

		PushMessagePayload pushMessagePayload = new PushMessagePayload("kemo@ebay.com", "Hello", "heya", details);
		GCMRequester gcmRequester = new GCMRequester(new PushHostInfo("123", "123", "123"), "normal");
		HttpPost httpPost = gcmRequester.build(pushMessagePayload, DEVICE_TOKEN, NOTIFICATION_TITLE);

		Assert.assertNotNull(EntityUtils.toString(httpPost.getEntity()));
	}
}
