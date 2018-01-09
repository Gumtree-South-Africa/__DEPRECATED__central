package com.ecg.messagecenter.pushmessage;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.ecg.messagecenter.pushmessage.PushService.Result;

@RunWith(MockitoJUnitRunner.class)
public class BoltPushServiceTest {
	
	private BoltPushService boltPushService;
	
	PushMessagePayload pushMessagePayload;
	
	private static final String receiverUserId = "123123";
	private static final String notificationType = "CHATMESSAGE";
	private static final String message = "how are you";
	private static final String toEmail = "kemo@ebay.com";
	private static final String adId = "123123";
	private static final String adTitle = "buying power";
	private static final String adImage = "http://www.ebay.com/image1.jpg";
	private static final String conversationId = "123123";
	private static final String badge = "3";
	private static final String alertId = "123123";
	private static final String notificationId = "123123";
	
    @Before
    public void init() {

    }
	
	@Test
	public void testBoltPushService() throws Exception {
		
        Map<String,String> pushMap = new HashMap<String, String>();
    	pushMap.put("conversationId", conversationId);
    	pushMap.put("receiverId", receiverUserId);
    	pushMap.put("adId", adId);
    	pushMap.put("adTitle", adTitle);
    	pushMap.put("badge", badge);
    	pushMap.put("locale", "en_ZA");
        pushMap.put("adImage", adImage);                
    	
        PushMessagePayload pushMessagePayload = new PushMessagePayload(toEmail, message, notificationType, pushMap); 
		BoltPushService boltPushService = new BoltPushService("http://abc.com", 123);
		Result result = boltPushService.sendPushMessage(pushMessagePayload);
		
		assertEquals("ERROR", result.getStatus().name());
	
	}
		
}