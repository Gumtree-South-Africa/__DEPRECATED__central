package com.ecg.replyts.core.runtime.listener;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.stereotype.Component;

@Component
public class MessageStatsListener implements MessageProcessedListener {
    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (messageWasModerated(message)) {
            logTransition(message.getState(), message.getHumanResultState());
        } else {
            logNewMessage(message.getState());
        }
    }

    private static boolean messageWasModerated(Message message) {
        return !message.getHumanResultState().equals(ModerationResultState.UNCHECKED);
    }

    private static void logNewMessage(MessageState messageState) {
        TimingReports.newCounter(asKey(messageState.name())).inc();
    }

    private static String asKey(String name) {
        return "processed." + name;
    }

    private static void logTransition(MessageState from, ModerationResultState to) {
        TimingReports.newCounter(asKey(from.name() + "-" + to.name())).inc();
    }
}
