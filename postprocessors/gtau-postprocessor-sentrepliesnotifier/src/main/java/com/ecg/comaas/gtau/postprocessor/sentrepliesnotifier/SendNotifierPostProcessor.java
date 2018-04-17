package com.ecg.comaas.gtau.postprocessor.sentrepliesnotifier;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.prometheus.ExternalServiceType;
import com.ecg.replyts.core.runtime.prometheus.PrometheusFailureHandler;
import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.stream.Collectors;

@ComaasPlugin
@Component
public class SendNotifierPostProcessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SendNotifierPostProcessor.class);
    private static final String ALWAYS_NOTIFY_FLAG = "always-notify-flag";
    private static final String XML_BODY_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

    private final CloseableHttpClient httpClient;
    private final String endpointUrl;

    @Autowired
    public SendNotifierPostProcessor(
            @Value("${replyts.sendnotifier.endpoint.url}") String endpointUrl,
            @Value("${replyts.sendnotifier.connectionTimeout:1000}") int connectionTimeout,
            @Value("${replyts.sendnotifier.connectionManagerTimeout:1000}") int connectionManagerTimeout,
            @Value("${replyts.sendnotifier.socketTimeout:1000}") int socketTimeout,
            @Value("${replyts.sendnotifier.maxConnectionsPerRoute:100}") int maxConnectionsPerRoute,
            @Value("${replyts.sendnotifier.maxConnections:100}") int maxConnections
    ) {
        this.httpClient = HttpClientFactory.createCloseableHttpClient(connectionTimeout, connectionManagerTimeout,
                socketTimeout, maxConnectionsPerRoute, maxConnections);
        this.endpointUrl = endpointUrl;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        // Only if the message is from a buyer to a seller
        if (MessageDirection.BUYER_TO_SELLER != context.getMessageDirection()) {
            return;
        }

        Conversation conversation = context.getConversation();
        // Notify either if it is first message in conversation or notify flag is true
        if (conversation.getMessages().size() == 1 || isNotifyOverride(conversation)) {
            // check whether this is an XML formatted message used for posting to Autogate
            String bodyText = context.getOutgoingMail().getTextParts(false).stream()
                    .map(TypedContent::getContent)
                    .collect(Collectors.joining());
            if (!bodyText.contains(XML_BODY_CONTENT)) {
                doGet(buildNotifyUrl(conversation));
            } else {
                LOG.trace("Message body contains XML content - ignoring this conversation id [{}], message id [{}] to prevent double counting of replies",
                        conversation.getId(), context.getMessageId());
            }
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private void doGet(String notifyUrl) {
        try {
            HttpGet httpGet = new HttpGet(notifyUrl);
            CloseableHttpResponse response = httpClient.execute(httpGet);
            if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                LOG.warn("HTTP GET to {} returned status code {} instead of {}", notifyUrl, response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
            }
        } catch (IOException e) {
            PrometheusFailureHandler.reportExternalServiceFailure(ExternalServiceType.NOTIFY_POST_PROCESSOR);
            LOG.error("HTTP GET to {} failed", notifyUrl, e);
        }
    }

    private String buildNotifyUrl(Conversation conversation) {
        StringBuilder sb = new StringBuilder(endpointUrl);
        sb.append("?adId=");
        sb.append(conversation.getAdId());
        // Let them know we were told to always notify in case they want to track it.
        if (isNotifyOverride(conversation)) {
            sb.append("&o=true");
        }
        String result = sb.toString();
        LOG.trace("Notify URL [{}].", result);
        return result;
    }

    private boolean isNotifyOverride(Conversation conversation) {
        LOG.trace("Conversation custom values [{}].", conversation.getCustomValues());
        Object isNotifyOverrideFlag = conversation.getCustomValues().get(ALWAYS_NOTIFY_FLAG);
        LOG.trace("Always notify flag [{}].", isNotifyOverrideFlag);
        return (isNotifyOverrideFlag != null && Boolean.valueOf(isNotifyOverrideFlag.toString()));
    }

    @PreDestroy
    public void preDestroy() {
        HttpClientFactory.closeWithLogging(httpClient);
    }
}
