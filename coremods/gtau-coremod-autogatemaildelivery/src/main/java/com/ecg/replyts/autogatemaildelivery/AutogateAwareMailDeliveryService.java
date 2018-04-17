package com.ecg.replyts.autogatemaildelivery;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import com.ecg.replyts.core.runtime.mailparser.MailEnhancer;
import com.ecg.replyts.core.runtime.prometheus.ExternalServiceType;
import com.ecg.replyts.core.runtime.util.HttpClientFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.nio.charset.Charset;

import static com.ecg.replyts.core.runtime.prometheus.PrometheusFailureHandler.reportExternalServiceFailure;

/**
 * Mail Delivery Service performs HTTP Posts of Leads to Autogate dataconnect service when the
 * specific URL header has been set - otherwise sends as normal mail message.
 */
@ComaasPlugin
@Primary
@Component
public class AutogateAwareMailDeliveryService implements MailDeliveryService {

    private static final Logger LOG = LoggerFactory.getLogger(AutogateAwareMailDeliveryService.class);

    private final String autogateHttpUrlHeader;
    private final String autogateHttpAccountName;
    private final String autogateHttpAccountPassword;
    private final CloseableHttpClient httpClient;
    private final MailDeliveryService smtpMailDeliveryService;
    private final MailEnhancer mailEnhancer;

    @Autowired
    public AutogateAwareMailDeliveryService(
            @Qualifier("smtpMailDeliveryService") MailDeliveryService smtpMailDeliveryService,
            MailEnhancer mailEnhancer,
            @Value("${replyts.autogate.header.url:X-Cust-Http-Url}") String autogateHttpUrlHeader,
            @Value("${replyts.autogate.header.account:X-Cust-Http-Account-Name}") String autogateHttpAccountName,
            @Value("${replyts.autogate.header.password:X-Cust-Http-Account-Password}") String autogateHttpAccountPassword,
            @Value("${replyts.autogate.httpclient.proxyHost:}") String proxyHost,
            @Value("${replyts.autogate.httpclient.proxyPort:80}") int proxyPort,
            @Value("${replyts.autogate.httpclient.maxConnectionsPerRoute:100}") int maxConnectionsPerRoute,
            @Value("${replyts.autogate.httpclient.maxConnections:100}") int maxConnections,
            @Value("${replyts.autogate.httpclient.connectionTimeout:1000}") int connectionTimeout,
            @Value("${replyts.autogate.httpclient.connectionManagerTimeout:1000}") int connectionManagerTimeout,
            @Value("${replyts.autogate.httpclient.socketTimeout:1000}") int socketTimeout
    ) {
        this.smtpMailDeliveryService = smtpMailDeliveryService;
        this.mailEnhancer = mailEnhancer;
        this.autogateHttpAccountName = autogateHttpAccountName;
        this.autogateHttpAccountPassword = autogateHttpAccountPassword;
        this.autogateHttpUrlHeader = autogateHttpUrlHeader;

        if (StringUtils.isEmpty(proxyHost)) {
            httpClient = HttpClientFactory.createCloseableHttpClient(connectionTimeout, connectionManagerTimeout,
                    socketTimeout, maxConnectionsPerRoute, maxConnections);
        } else {
            httpClient = HttpClientFactory.createCloseableHttpClientWithProxy(connectionTimeout, connectionManagerTimeout,
                    socketTimeout, maxConnectionsPerRoute, maxConnections, proxyHost, proxyPort);
        }
    }

    @Override
    public void deliverMail(Mail mail) throws MailDeliveryException {
        LOG.info("Got headers: {}", mail.getDecodedHeaders().keySet());
        String xRobot = mail.getUniqueHeader("X-Robot");
        String postUrl = mail.getUniqueHeader(autogateHttpUrlHeader);
        if (StringUtils.isEmpty(postUrl)) {
            LOG.info("Delivering mail via ReplyTS");
            if (xRobot != null) {
                LOG.info("X-Robot header present, not sending mail");
            } else {
                smtpMailDeliveryService.deliverMail(this.mailEnhancer.process(mail));
            }
        } else {
            postHttpLead(mail, postUrl);
        }
    }

    /**
     * Send HTTP Post lead to the professional sellers
     */
    private void postHttpLead(Mail mail, String postUrl) throws MailDeliveryException {
        try {
            LOG.info("Autogate HTTP found - posting Lead to Autogate url {}", postUrl);

            // set headers
            final HttpPost httpPost = new HttpPost(postUrl);
            httpPost.setHeader("AccountName", mail.getUniqueHeader(autogateHttpAccountName));
            httpPost.setHeader("Password", mail.getUniqueHeader(autogateHttpAccountPassword));

            // set body
            final StringBuilder sb = new StringBuilder();
            for (TypedContent<String> textPart : mail.getTextParts(false)) {
                sb.append(textPart.getContent());
            }
            final StringEntity se = new StringEntity(sb.toString(), Charset.forName("UTF-8"));
            se.setContentType("text/xml");
            httpPost.setHeader("Content-Type", "text/xml;charset=UTF-8");
            httpPost.setEntity(se);

            // perform HTTP post
            LOG.info("Performing post to to Autogate");
            final HttpResponse response = httpClient.execute(httpPost);

            // check results
            final HttpEntity httpEntity = response.getEntity();
            String httpEntityString = null;
            if (httpEntity != null) {
                httpEntityString = EntityUtils.toString(httpEntity);
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
                    response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                LOG.info("Lead successfully posted to Autogate, messageId={}, responseCode={}, response={}",
                        mail.getMessageId(), response.getStatusLine().getStatusCode(), httpEntityString);
            } else {
                reportExternalServiceFailure(ExternalServiceType.AUTO_GATE);
                String requestBody = sb.toString();
                LOG.error("Failed to post Lead to Autogate, messageId={}, responseCode={}, requestBody={}, response={}",
                        mail.getMessageId(), response.getStatusLine().getStatusCode(), requestBody, httpEntityString);
            }
        } catch (Exception e) {
            reportExternalServiceFailure(ExternalServiceType.AUTO_GATE);
            String errorMessage = "Failed to deliver mail messageId=" + mail.getMessageId();
            LOG.error(errorMessage, e);
            throw new MailDeliveryException(errorMessage, e);
        }
    }

    @PreDestroy
    public void preDestroy() {
        HttpClientFactory.closeWithLogging(httpClient);
    }
}
