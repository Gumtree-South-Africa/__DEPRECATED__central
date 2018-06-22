package com.ecg.comaas.core.filter.remote;

import com.ecg.comaas.core.filter.remote.api.FilterGrpc;
import com.ecg.comaas.core.filter.remote.api.FilterRequest;
import com.ecg.comaas.core.filter.remote.api.FilterResponse;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.google.protobuf.Timestamp;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class RemoteFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteFilter.class);

    private final String instanceName;
    private final Duration filterTimeout;
    private final FilterGrpc.FilterFutureStub remoteFilter;

    RemoteFilter(String instanceName, String endpoint, Duration filterTimeout) {
        this.instanceName = instanceName;
        // todo kobyakov: should be closed somehow on the filter recreation
        ManagedChannel channel = ManagedChannelBuilder.forTarget(endpoint).usePlaintext(true).build();
        this.filterTimeout = filterTimeout;
        this.remoteFilter = FilterGrpc.newFutureStub(channel);
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext ctx) throws ProcessingTimeExceededException {
        Future<FilterResponse> future = remoteFilter.apply(fromContext(ctx));
        try {
            return translateResponse(extractSender(ctx), future.get(filterTimeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            LOG.warn("{} interrupted while waiting for the remote filter reply", instanceName);
            return Collections.emptyList();
        } catch (ExecutionException e) {
            LOG.error("{} invocation failed", instanceName, e);
            return Collections.emptyList();
        } catch (TimeoutException e) {
            LOG.error("{} invocation did not complete within the period of {}", instanceName, filterTimeout);
            future.cancel(true);
            return Collections.emptyList();
        }
    }

    private FilterRequest fromContext(MessageProcessingContext ctx) {
        return FilterRequest.newBuilder()
                .setCorrelationId(MDC.get(MDCConstants.CORRELATION_ID))
                .setProcessingDeadline(calculateProcessingDeadline())
                .setSenderId(extractSender(ctx))
                .setReceiverId(extractReceiver(ctx))
                .setConversationId(ctx.getConversation().getId())
                .setMessageId(ctx.getMessageId())
                .setMessage(ctx.getMessage().getPlainTextBody())
                .putAllMetadata(ctx.getMessage().getHeaders())
                .build();
    }

    private Timestamp calculateProcessingDeadline() {
        Instant deadlineInstant = Instant.now().plus(filterTimeout);
        return Timestamp.newBuilder().setSeconds(deadlineInstant.getEpochSecond()).setNanos(deadlineInstant.getNano()).build();
    }

    private static String extractSender(MessageProcessingContext ctx) {
        Message message = ctx.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        return ctx.getConversation().getUserId(fromRole);
    }

    private static String extractReceiver(MessageProcessingContext ctx) {
        Message message = ctx.getMessage();
        ConversationRole toRole = message.getMessageDirection().getToRole();
        return ctx.getConversation().getUserId(toRole);
    }

    private List<FilterFeedback> translateResponse(String senderId, FilterResponse response) {
        return Collections.singletonList(
                new FilterFeedback(senderId, response.getDescription(), response.getScore(), FilterResultState.OK)
        );
    }
}