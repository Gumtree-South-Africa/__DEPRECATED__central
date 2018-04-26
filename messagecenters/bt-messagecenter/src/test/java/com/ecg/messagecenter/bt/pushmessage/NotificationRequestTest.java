package com.ecg.messagecenter.bt.pushmessage;

import static org.junit.Assert.assertEquals;

import com.ecg.messagecenter.bt.pushmessage.MetaChatMessage;
import com.ecg.messagecenter.bt.pushmessage.NotificationRequest;
import org.junit.Test;

public class NotificationRequestTest {

    @Test
    public void test() {
    	
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setToEmail("abc@email.com");
        notificationRequest.setMessage("this is testing");
        
        MetaChatMessage meta = new MetaChatMessage();
        meta.setAdId("adId");
        meta.setAdThumbNail("adImage");
        meta.setAdTitle("adTitle");
        meta.setBadge("badge");
        meta.setConversationId("conversationId");
        meta.setReceiverUserId("123143");
        notificationRequest.setMeta(meta);
    }
}
