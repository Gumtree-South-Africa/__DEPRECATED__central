package com.ecg.replyts.core.runtime.persistence.conversation;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.command.NewConversationCommand;
import com.ecg.replyts.core.api.model.conversation.event.ConversationCreatedEvent;
import com.google.common.collect.Maps;
import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author mhuttar
 */
public class ConversationSecretPayloadEditorTest {

    ConversationSecretPayloadEditor editor = new ConversationSecretPayloadEditor();

    @Test
    public void convertsToJsonAndBack() throws Exception {
        ConversationCreatedEvent evt = new ConversationCreatedEvent("confid", "adid", "buyerid", "sellerid", "buyersecret", "sellersecret", DateTime.now(), ConversationState.ACTIVE, Maps.<String, String>newHashMap());
        NewConversationCommand restored = editor.fromJson(editor.toJson(evt));

        assertEquals(evt.getConversationId(), restored.getConversationId());
        assertEquals(evt.getAdId(), restored.getAdId());
        assertEquals(evt.getSellerId(), restored.getSellerId());
        assertEquals(evt.getBuyerId(), restored.getBuyerId());
        assertEquals(evt.getSellerSecret(), restored.getSellerSecret());
        assertEquals(evt.getBuyerSecret(), restored.getBuyerSecret());

        // always restore to conv state active --> this is only for datacenter crash mode 100% correct handling in edge cases is not required.
        assertEquals(ConversationState.ACTIVE, restored.getState());
    }
}
