package com.ecg.replyts.core.runtime.listener;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.runtime.TimingReports;

/**
 * User: acharton
 * Date: 4/25/13
 */
public class MessageStatsListener implements MessageProcessedListener {


    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (messageWasModerated(message)) {
            logTransition(message.getState(), message.getHumanResultState());
        } else {
            logNewMessage(message.getState());
        }
    }

    private boolean messageWasModerated(Message message) {
        return !message.getHumanResultState().equals(ModerationResultState.UNCHECKED);
    }

    private void logNewMessage(MessageState messageState) {
        Counter counter = TimingReports.newCounter(asKey(messageState.name()));
        counter.inc();
    }

    static String asKey(String name) {
        return "processed." + name;
    }

    private void logTransition(MessageState from, ModerationResultState to) {
        Counter counter = TimingReports.newCounter(asKey(from.name() + "-" + to.name()));
        counter.inc();
    }


}
