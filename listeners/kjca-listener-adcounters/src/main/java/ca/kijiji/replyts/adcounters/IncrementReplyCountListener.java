package ca.kijiji.replyts.adcounters;

import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
class IncrementReplyCountListener implements MessageProcessedListener{
    private static final Logger LOG = LoggerFactory.getLogger(IncrementReplyCountListener.class);

    private TnsApiClient tnsApiClient;

    @Autowired
    IncrementReplyCountListener(TnsApiClient tnsApiClient) {
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message){
        if (message.getState() == MessageState.SENT) {
            try {
                tnsApiClient.incrementReplyCount(conversation.getAdId());
                LOG.debug("Request for incrementing Ad({}) reply count has completed.", conversation.getAdId());
            } catch (Exception e) {
                LOG.error("Increment reply count failed for Ad {} ", conversation.getAdId(), e);
            }
        }
    }
}
