package com.ebay.ecg.replyts.robot.handler;

import com.ebay.ecg.australia.events.command.robot.RobotCommands;
import com.ebay.ecg.australia.events.entity.Entities;
import com.ebay.ecg.australia.events.service.EventHandler;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessageSender;
import com.ebay.ecg.replyts.robot.api.requests.payload.RichMessage;
import com.ebay.ecg.replyts.robot.service.RobotService;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQConsumer implements EventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQConsumer.class);

    private final RobotService robotService;

    @Autowired
    public RabbitMQConsumer(RobotService robotService) {
        this.robotService = robotService;
    }

    private static MessageDirection convert(Entities.MessageDirection messageDirection) {
        if (Entities.MessageDirection.BUYER_TO_SELLER == messageDirection) {
            return MessageDirection.BUYER_TO_SELLER;
        } else if (Entities.MessageDirection.SELLER_TO_BUYER == messageDirection) {
            return MessageDirection.SELLER_TO_BUYER;
        } else {
            return MessageDirection.UNKNOWN;
        }
    }

    private static MessageSender sender(Entities.MessageInfo messageInfo) {
        if (messageInfo.hasSender()) {
            Entities.MessageSenderInfo senderInfo = messageInfo.getSender();
            return new MessageSender(senderInfo.getName(), senderInfo.getIconList());
        }
        return null;
    }

    private static RichMessage richMessage(Entities.MessageInfo messageInfo) {
        if (messageInfo.hasRichContentMessage()) {
            Entities.RichTextMessageInfo richContentMessage = messageInfo.getRichContentMessage();
            return new RichMessage(richContentMessage.getMessage(), richContentMessage.getLinksList());
        }
        return null;
    }

    @Override
    public void fire(Object e) {
        if (e instanceof GeneratedMessage) {
            MDCConstants.setTaskFields(RabbitMQConsumer.class.getSimpleName());
            GeneratedMessage message = (GeneratedMessage) e;
            String messageClassName = message.getClass().getName();
            if (messageClassName.contains(RobotCommands.PostMessageCommand.class.getSimpleName())) {
                LOG.debug("Command message received {}:{}", messageClassName, message);

                Entities.MessageInfo messageInfo = ((RobotCommands.PostMessageCommand) message).getMessageInfo();

                MessagePayload payload = new MessagePayload(messageInfo.getMessage(), convert(messageInfo.getMessageDirection()),
                        messageInfo.getLinksList(), sender(messageInfo), richMessage(messageInfo));

                robotService.addMessageToConversation(messageInfo.getConversationId(), payload);
            }
        }
    }
}
