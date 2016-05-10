package com.ebay.ecg.replyts.robot.handler;

import com.codahale.metrics.Timer;
import com.ebay.ecg.australia.events.command.robot.RobotCommands;
import com.ebay.ecg.australia.events.event.robot.RobotEvents;
import com.ebay.ecg.australia.events.service.EventHandler;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ebay.ecg.replyts.robot.service.RobotService;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by nmao on 11/01/2016.
 */
@Component
public class RabbitMQConsumer implements EventHandler {

    private static final Timer CONSUMER_ROBOT_POST_TO_CONVERSATION_BY_ID = TimingReports.newTimer("consumer-robot-post-to-conversation-by-id");

    private static Logger LOG = LoggerFactory.getLogger(RabbitMQConsumer.class.getCanonicalName());

    private final RobotService robotService;

    @Autowired
    public RabbitMQConsumer(RobotService robotService) {
        this.robotService = robotService;
    }

    @Override
    public void fire(Object e) {
        if (e instanceof GeneratedMessage) {
            GeneratedMessage message = (GeneratedMessage) e;
            try {
                if (message.getClass().getName().contains(RobotCommands.PostMessageCommand.class.getSimpleName())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Command message received " + message.getClass().getName() + ":" + message.toString());
                    }

                    Timer.Context timerContext = CONSUMER_ROBOT_POST_TO_CONVERSATION_BY_ID.time();

                    RobotCommands.PostMessageCommand postMessageCommand = (RobotCommands.PostMessageCommand)message;

                    MessagePayload payload = new MessagePayload();
                    payload.setMessage(postMessageCommand.getMessageInfo().getMessage());
                    payload.setMessageDirection(postMessageCommand.getMessageInfo().getMessageDirection().toString());
                    payload.replaceLinks(postMessageCommand.getMessageInfo().getLinksList());

                    try {
                        robotService.addMessageToConversation(postMessageCommand.getMessageInfo().getConversationId(), payload);
                    } finally {
                        timerContext.stop();
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Message " + message.getClass().getName() + " written successfully");
                    }
                }
            } catch (Exception ex) {
                LOG.error("Error writing message to event log", ex);
                throw new RuntimeException(ex);
            }
        }
    }
}
