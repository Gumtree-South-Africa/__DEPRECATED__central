package com.ecg.messagecenter.listeners;

import ch.qos.logback.core.encoder.EncoderBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.runtime.listener.MessageProcessedListener;
import org.apache.commons.lang.LocaleUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;

@Component
public class MessageLoggerListener implements MessageProcessedListener {
    private static final long LOG_VERSION_NUM = 4L;
    private static final String DELIMITER = "\u0001";

    private RollingFileAppender<String> auditLogAppender;

    // XXX: Until we move to writing directly to Flume, we'll circumvent the logging system by calling the appender directly

    @Autowired
    public MessageLoggerListener(@Value("${logDir:.}") String logDir) {
        auditLogAppender = new RollingFileAppender<>();

        auditLogAppender.setFile(format("%s/replyts-audit.log", logDir));

        TimeBasedRollingPolicy rollingPolicy = new TimeBasedRollingPolicy<>();

        rollingPolicy.setFileNamePattern(format("%s/archived/replyts-audit.log", logDir));
        rollingPolicy.setMaxHistory(7);

        rollingPolicy.start();

        auditLogAppender.setRollingPolicy(rollingPolicy);

        auditLogAppender.setEncoder(new EncoderBase<String>() {
            @Override
            public void doEncode(String message) throws IOException {
                this.outputStream.write(format("%s\n", message).getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public void close() throws IOException {
                // Do nothing
            }
        });

        auditLogAppender.start();
    }

    @Override
    public void messageProcessed(Conversation conversation, Message message) {
        if (message.getState() != MessageState.SENT) {
            return;
        }

        // Log format is specific for Bolt and picked up by Logstash for storage in HDFS

        String localeStr = conversation.getCustomValues().get("locale");

        String country = "";
        String brandCode = "";

        if (!StringUtils.isEmpty(localeStr)) {
            Locale locale = LocaleUtils.toLocale(localeStr);

            country = locale.getCountry();
            brandCode = locale.getVariant();
        }

        int msgDirection = message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER ? 0 : 1;

        String fromEmail = msgDirection == 0 ? conversation.getBuyerId() : conversation.getSellerId();
        String toEmail = msgDirection == 0 ? conversation.getSellerId() : conversation.getBuyerId();

        DateTime replyDate = message.getReceivedAt();
        DateTimeFormatter fmt = DateTimeFormat.forPattern("dd/MM/yyyy");

        Map<String, String> headers = message.getHeaders();

        final StringBuilder logBuf = new StringBuilder();

        logBuf.append(LOG_VERSION_NUM).append(DELIMITER)
          .append(System.currentTimeMillis()).append(DELIMITER)
          .append(country).append(DELIMITER)
          .append(conversation.getAdId()).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Ip"))).append(DELIMITER)
          .append(normalize(fromEmail)).append(DELIMITER)
          .append(normalize(conversation.getCustomValues().get("buyerphone"))).append(DELIMITER)
          .append(normalize(conversation.getCustomValues().get("buyer-name"))).append(DELIMITER)
          .append(message.getPlainTextBody()).append(DELIMITER)
          .append("").append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Template"))).append(DELIMITER)
          .append(brandCode).append(DELIMITER)
          .append("").append(DELIMITER)
          .append(fmt.print(replyDate)).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Hw-Vendor"))).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Hw-Family"))).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Hw-Name"))).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Hw-Model"))).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Platform-Vendor"))).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Platform-Name"))).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Platform-Version"))).append(DELIMITER)
          .append(normalize(conversation.getCustomValues().get("mc-sellerid"))).append(DELIMITER)
          .append(message.getId()).append(DELIMITER)
          .append(normalize(toEmail)).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Source"))).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-Deviceid"))).append(DELIMITER)
          .append(msgDirection).append(DELIMITER)
          .append(conversation.getId()).append(DELIMITER)
          .append(normalize(headers.get("X-Cust-User-Agent")));

        // XXX: This is being written to the log on disk to be picked up by Logstash and then consumed by Flume.
        // XXX: Once we cut over to the COMaaS cloud environment, this should be published to Kafka and consumed by
        // XXX: the cloud Flume instances from Kafka; and then published to (the same) HDFS instance

        String auditLine = logBuf.toString().replaceAll("(\\t|\\n|\\r)", " ");

        auditLogAppender.doAppend(auditLine);
    }

    private String normalize(String value) {
        return StringUtils.isEmpty(value) ? "" : value;
    }
}