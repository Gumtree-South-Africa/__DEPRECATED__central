package com.ecg.messagecenter.listeners;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.SimplePostBoxInitializer;
import com.ecg.messagecenter.pushmessage.AdInfoLookup;
import com.ecg.messagecenter.pushmessage.KmobilePushService;
import com.ecg.messagecenter.pushmessage.PushMessageOnUnreadConversationCallback;
import com.ecg.messagecenter.pushmessage.PushService;
import com.ecg.messagecenter.util.MessageContentHelper;
import com.ecg.messagecenter.util.MessageType;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.TimingReports;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import static com.ecg.messagecenter.util.MessageType.isAutogate;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 17:33
 *
 * @author maldana@ebay.de
 */
public class PostBoxUpdateListener implements MessageProcessedListener {

    private static final Timer PROCESSING_TIMER = TimingReports.newTimer("message-box.postBoxUpdateListener.timer");
    private static final Counter PROCESSING_SUCCESS = TimingReports.newCounter("message-box.postBoxUpdateListener.success");
    private static final Counter PROCESSING_FAILED = TimingReports.newCounter("message-box.postBoxUpdateListener.failed") ;
    private static final Counter MESSGAE_XML = TimingReports.newCounter("message-box.postBoxUpdateListener.xml");
    private static final Counter MESSGAE_AUTOGATE = TimingReports.newCounter("message-box.postBoxUpdateListener.autogate") ;
    private static final String INITIAL_CONVERSATION_MESSAGE = "Gumtree prompted this buyer";

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxUpdateListener.class);

    private final UserNotificationRules userNotificationRules;
    private final SimplePostBoxInitializer postBoxInitializer;
    private final PushService pushService;
    private final AdInfoLookup adInfoLookup;

    @Autowired
    public PostBoxUpdateListener(SimplePostBoxInitializer postBoxInitializer,
                                 @Value("${push-mobile.enabled:true}") boolean pushEnabled,
                                 @Value("${push-mobile.host:}") String pushHost,
                                 @Value("${push-mobile.port:80}") Integer pushPort,
                                 @Value("${api.image.lookup.enabled:false}") Boolean apiEnabled,
                                 @Value("${api.host:api.gumtree.com.au}") String apiHost,
                                 @Value("${api.port:80}") Integer apiPort,
                                 @Value("${api.connectionTimeout:1500}") Integer connectionTimeout,
                                 @Value("${api.connectionManagerTimeout:1500}") Integer connectionManagerTimeout,
                                 @Value("${api.socketTimeout:2500}") Integer socketTimeout,
                                 @Value("${api.maxConnectionsPerHost:40}") Integer maxConnectionsPerHost,
                                 @Value("${api.maxConnectionsPerHost:40}") Integer maxTotalConnections) {
        this.postBoxInitializer = postBoxInitializer;
        this.adInfoLookup = new AdInfoLookup(apiHost, apiPort, connectionTimeout, connectionManagerTimeout, socketTimeout, maxConnectionsPerHost, maxTotalConnections);
        if (pushEnabled && !Strings.isNullOrEmpty(pushHost)) {
            this.pushService = new KmobilePushService(pushHost, pushPort);
        } else {
            this.pushService = null;
        }
        this.userNotificationRules = new UserNotificationRules();
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {

        if (conversation.getState() == ConversationState.DEAD_ON_ARRIVAL) {
            return;
        }

        if(isAutogate(message)) {
            MESSGAE_AUTOGATE.inc();
        }

        if (MessageContentHelper.isLooksLikeXml(message.getPlainTextBody())) {
            MESSGAE_XML.inc();
            return;
        }

        Timer.Context timerContext = PROCESSING_TIMER.time();

        try {

            if (conversation.getSellerId() == null || conversation.getBuyerId() == null) {
                LOG.info(
                        String.format(
                                "No seller or buyer email available for conversation #%s and conv-state %s and message #%s",
                                conversation.getId(),
                                conversation.getState(),
                                message.getId()));
                return;
            }

            // We don't broadcast robot messages, to do so UserNotificationRules needs to change
            if(MessageType.isRobot(message)) {
                if(message.getMessageDirection().equals(MessageDirection.BUYER_TO_SELLER)) {
                    if(!message.getPlainTextBody().contains(INITIAL_CONVERSATION_MESSAGE))
                        updateMessageCenter(conversation.getSellerId(), conversation, message, userNotificationRules.sellerShouldBeNotified(message));
                }
                if(message.getMessageDirection().equals(MessageDirection.SELLER_TO_BUYER)) {
                    updateMessageCenter(conversation.getBuyerId(), conversation, message, userNotificationRules.buyerShouldBeNotified(message));
                }
            } else {
                updateMessageCenter(conversation.getSellerId(), conversation, message, userNotificationRules.sellerShouldBeNotified(message));
                updateMessageCenter(conversation.getBuyerId(), conversation, message, userNotificationRules.buyerShouldBeNotified(message));
            }



            PROCESSING_SUCCESS.inc();

        } catch(Exception e) {
            PROCESSING_FAILED.inc();
            throw new RuntimeException("Error with post-box syncing " + e.getMessage(),e);
        } finally{
            timerContext.stop();
        }
    }


    private void updateMessageCenter(String email, Conversation conversation, Message message, boolean newReplyArrived) {
        postBoxInitializer.moveConversationToPostBox(
                email,
                conversation,
                newReplyArrived,
                new PushMessageOnUnreadConversationCallback(
                        pushService,
                        adInfoLookup,
                        conversation,
                        message
                ));
    }


}
