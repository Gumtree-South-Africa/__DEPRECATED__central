package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.comaas.filterapi.dto.FilterFeedback.ResultStateEnum;
import com.ecg.comaas.filterapi.dto.FilterRequest;
import com.ecg.comaas.filterapi.dto.FilterResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vavr.control.Try;
import org.joda.time.DateTime;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

// just a namespace for the mappers to and from the DTO
public interface FilterAPIMapper {

    /**
     * returns serializer that does it's job properly (also with date fields)
     */
    static ObjectMapper getSerializer() {
        return FilterAPIMapper.FromModel.objectMapper;
    }

    class FromModel {
        private static final ObjectMapper objectMapper;

        static {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }


        public static String toSenderUserId(MessageProcessingContext ctx) {
            Message message = ctx.getMessage();
            ConversationRole fromRole = message.getMessageDirection().getFromRole();
            return ctx.getConversation().getUserId(fromRole);
        }

        public static FilterRequest toFilterRequest(MessageProcessingContext ctx, String correlationId, int maxProcessingDurationMillis) {
            Objects.requireNonNull(ctx);

            Message msgIn = ctx.getMessage();

            return new FilterRequest()
                    .correlationId(correlationId)
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
                                    .receivedTime(jodaToJavaDatetime(ctx.getMessage().getReceivedAt()))
                    );
        }

        private static OffsetDateTime jodaToJavaDatetime(DateTime jodaDateTime) {
            return OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(jodaDateTime.getMillis()),
                    jodaDateTime.getZone().toTimeZone().toZoneId()
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

        public static FilterResultState toModelResultState(ResultStateEnum dto) {
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
