package com.ecg.replyts.app.preprocessorchain.preprocessors;

import com.ecg.replyts.app.preprocessorchain.PreProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import com.ecg.replyts.core.runtime.persistence.EmailOptOutRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.processing.MessageProcessingContext.DELIVERY_CHANNEL_MAIL;

@Component
@ConditionalOnProperty(value = "email.opt.out.enabled", havingValue = "true")
public class EmailOptOutPreProcessor implements PreProcessor {

    private EmailOptOutPreProcessorFilter filter;
    private final EmailOptOutRepository emailOptOutRepo;
    private final UserIdentifierService userIdService;

    @Autowired
    public EmailOptOutPreProcessor(EmailOptOutRepository emailOptOutRepo, UserIdentifierService userIdService) {
        this.emailOptOutRepo = emailOptOutRepo;
        this.userIdService = userIdService;
    }

    @Override
    public void preProcess(MessageProcessingContext context) {
        if (filter != null && filter.filter(context)) {
            return;
        }

        if (context.getMail().isPresent()) {
            Conversation c = context.getConversation();
            MessageDirection toRole = context.getMessageDirection();
            ConversationRole role = toRole.getToRole();

            userIdService.getUserIdentificationOfConversation(c, role).ifPresent(userId -> {
                if (!emailOptOutRepo.isEmailTurnedOn(userId)) {
                    context.skipDeliveryChannel(DELIVERY_CHANNEL_MAIL);
                }
            });
        } else {
            context.skipDeliveryChannel(DELIVERY_CHANNEL_MAIL);
        }
    }

    @Override
    public int getOrder() {
        return 40;
    }

    @Autowired(required = false)
    public void setEmailOptOutPreProcessorFilter(EmailOptOutPreProcessorFilter filter) {
        this.filter = filter;
    }
}