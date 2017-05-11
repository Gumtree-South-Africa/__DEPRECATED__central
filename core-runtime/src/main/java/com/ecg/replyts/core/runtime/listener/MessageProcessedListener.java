package com.ecg.replyts.core.runtime.listener;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;

/**
 * ReplyTS Event Listener that will be informed, whenever a message was processed.
 * Listeners are guaranteed to be informed on each message that was not terminated abnormally (by a runtime exception).
 * They are invoked at the very end of the processing flow, after the message/conversation have been updated to persistence.
 * <p/>
 * <h2>Registering a listener</h2>
 * To register a listener, one simply needs to create a Spring Bean implementing the MessageProcessedListener
 * in one of the *-rts-plugin.xml files that are in the classpath.
 * <br/>
 * Upon Startup, ReplyTS will log out all listeners, it knows of, similiar to this:
 * <blockquote>
 * INFO  3037     c.e.r.a.ProcessingConfiguration: With MessageProcessedListeners: class com.ecg.replyts.integration.test.AwaitMailSentProcessedListener
 * </blockquote>
 * <p/>
 * <p>
 * Listeners are invoked:
 * <ul>
 * <li>In the normal mail processing flow at the very end</li>
 * <li>In the Message moderated processing flow (when a CS agent moderated a message) after the changes were applied to the message</li>
 * </ul>
 * </p>
 */
public interface MessageProcessedListener {

    /**
     * invoked when a message was processed. parameters are the concerned message and it's conversation.
     * Please note that one message might be processed more than once, if it was blocked/held at first
     * but then moderated by CS agents.
     */
    void messageProcessed(Conversation conversation, Message message);

}