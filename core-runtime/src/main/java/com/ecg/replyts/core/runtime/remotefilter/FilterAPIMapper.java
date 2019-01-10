package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.comaas.filterapi.dto.FilterFeedback.ResultStateEnum;
import com.ecg.comaas.filterapi.dto.FilterRequest;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import io.vavr.control.Try;
import org.slf4j.MDC;

import com.ecg.comaas.filterapi.dto.FilterResponse;

import java.util.*;
import java.util.stream.Collectors;

// just a namespace for the mappers to and from the DTO
public interface FilterAPIMapper {

    class FromModel {

        public static String toSenderUserId(MessageProcessingContext ctx) {
            Message message = ctx.getMessage();
            ConversationRole fromRole = message.getMessageDirection().getFromRole();
            return ctx.getConversation().getUserId(fromRole);
        }

        public static FilterRequest toFilterRequest(MessageProcessingContext ctx, int maxProcessingDurationMillis) {
            Objects.requireNonNull(ctx);

            Message msgIn = ctx.getMessage();

            return new FilterRequest()
                    .correlationId(MDC.get(MDCConstants.CORRELATION_ID))
                    .maxProcessingDurationMs(maxProcessingDurationMillis)
                    .message(
                            new com.ecg.comaas.filterapi.dto.Message()
                                    // a *new* message will always have a UUID id.
                                    .messageId(UUID.fromString(msgIn.getId()))
                                    .messageMetadata(msgIn.getCaseInsensitiveHeaders())
                                    .messageText(msgIn.getPlainTextBody())
                                    .emailSubject(ctx.getMail().map(Mail::getSubject).orElse(null))
                                    .senderUserId(toSenderUserId(ctx))
                                    .conversationId(ctx.getConversation().getId())
                                    .conversationMetadata(ctx.getConversation().getCustomValues())
                    );
        }
    }

    class FromAPI {
        private static final Map<ResultStateEnum, FilterResultState> resultStates = new EnumMap<>(ResultStateEnum.class);

        static {
            resultStates.put(ResultStateEnum.OK, FilterResultState.OK);
            resultStates.put(ResultStateEnum.HELD, FilterResultState.HELD);
            resultStates.put(ResultStateEnum.DROPPED, FilterResultState.DROPPED);
            resultStates.put(ResultStateEnum.ACCEPT_AND_TERMINATE, FilterResultState.ACCEPT_AND_TERMINATE);
        }

        public static FilterResultState toModelResultState(ResultStateEnum dto){
            return resultStates.get(dto);
        }

        public static Try<List<FilterFeedback>> toFilterFeedback(FilterResponse fr) {
            // note that validating the presence of fields here (instead of with javax annotations on the DTO)
            // allows us to do https://martinfowler.com/bliki/TolerantReader.html
            return Try.of(() -> fr.getFeedback().stream()
                    .map(
                            dto -> new FilterFeedback(
                                    Objects.requireNonNull(dto.getUiHint()),
                                    Objects.requireNonNull(dto.getDescription(), "description required"),
                                    Objects.requireNonNull(dto.getScore(), "score required"),
                                    Objects.requireNonNull(toModelResultState(dto.getResultState()), "illegal result state")
                            )
                    )
                    .collect(Collectors.toList())
            );
        }
    }

}
