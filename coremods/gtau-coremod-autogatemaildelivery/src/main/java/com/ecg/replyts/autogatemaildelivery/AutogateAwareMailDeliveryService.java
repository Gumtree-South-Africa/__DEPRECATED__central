package com.ecg.replyts.autogatemaildelivery;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryException;
import com.ecg.replyts.core.runtime.maildelivery.MailDeliveryService;
import com.ecg.replyts.core.runtime.mailparser.MailEnhancer;
import com.google.common.base.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Mail Delivery Service performs HTTP Posts of Leads to Autogate dataconnect service when the 
 * specific URL header has been set - otherwise sends as normal mail message.
 */
@Primary
@Component
public class AutogateAwareMailDeliveryService implements MailDeliveryService {
    private final static Logger LOG = LoggerFactory.getLogger(AutogateAwareMailDeliveryService.class);

    private String autogateHttpUrlHeader;

    private String autogateHttpAccountName;

    private String autogateHttpAccountPassword;

    private HttpClient httpClient;

    private MailDeliveryService smtpMailDeliveryService;

    private MailEnhancer mailEnhancer;

    @Autowired
    public AutogateAwareMailDeliveryService(
            @Qualifier("smtpMailDeliveryService") MailDeliveryService smtpMailDeliveryService,
            MailEnhancer mailEnhancer,
            @Value("${replyts.autogate.header.url:X-Cust-Http-Url}")
            String autogateHttpUrlHeader,
            @Value("${replyts.autogate.header.account:X-Cust-Http-Account-Name}")
            String autogateHttpAccountName,
            @Value("${replyts.autogate.header.password:X-Cust-Http-Account-Password}")
            String autogateHttpAccountPassword,
            @Value("${replyts.autogate.httpclient.proxyHost:}")
            String proxyHost,
            @Value("${replyts.autogate.httpclient.proxyPort:80}")
            int proxyPort,
            @Value("${replyts.autogate.httpclient.maxConnectionsPerRoute:100}")
            int maxConnectionsPerRoute,
            @Value("${replyts.autogate.httpclient.maxConnections:100}")
            int maxConnections,
            @Value("${replyts.autogate.httpclient.connectionTimeout:1000}")
            int connectionTimeout,
            @Value("${replyts.autogate.httpclient.socketTimeout:1000}")
            int socketTimeout) {
        this.smtpMailDeliveryService = smtpMailDeliveryService;
        this.mailEnhancer = mailEnhancer;
        this.autogateHttpAccountName = autogateHttpAccountName;
        this.autogateHttpAccountPassword = autogateHttpAccountPassword;
        this.autogateHttpUrlHeader = autogateHttpUrlHeader;

        httpClient = buildHttpClient(proxyHost, proxyPort, socketTimeout, connectionTimeout, maxConnections, maxConnectionsPerRoute);
    }

    private HttpClient buildHttpClient(String proxyHost, int proxyPort, int socketTimeout, int connectionTimeout, int maxConnections, int maxConnectionsPerRoute) {
        HttpClientBuilder builder = HttpClientBuilder.createHttpclient();

        if(!Strings.isNullOrEmpty(proxyHost)) {
            builder.usingProxy(proxyHost, proxyPort);
        }

        return builder.withSocketTimeout(socketTimeout)
                .withConnectionTimeout(connectionTimeout)
                .withConnectionsLimitedTo(maxConnections, maxConnectionsPerRoute).build();
    }

    @Override
    public void deliverMail(Mail m) throws MailDeliveryException {
        LOG.info("Got headers: " + m.getDecodedHeaders().keySet());
        String xRobot = m.getUniqueHeader("X-Robot");
        String postUrl = m.getUniqueHeader(autogateHttpUrlHeader);
        if (postUrl == null || postUrl.isEmpty()) {
            LOG.info("Delivering mail via ReplyTS");
            if (xRobot != null) {
                LOG.info("X-Robot header present, not sending mail");
                return;
            } else {
                smtpMailDeliveryService.deliverMail(this.mailEnhancer.process(m));
            }
        } else {
            postHttpLead(m, postUrl);
        }
    }


    /**
     * Send HTTP Post lead to the professional sellers
     */
    private void postHttpLead(Mail m, String postUrl) throws MailDeliveryException {
        try {
            LOG.info("Autogate HTTP found - posting Lead to Autogate");

            // set headers
            final String accountName = m.getUniqueHeader(autogateHttpAccountName);
            final String accountPassword = m.getUniqueHeader(autogateHttpAccountPassword);
            final HttpPost httpPost = new HttpPost(postUrl);
            httpPost.setHeader("AccountName", accountName);
            httpPost.setHeader("Password", accountPassword);

            // set body
            final StringBuilder sb = new StringBuilder();
            for(TypedContent<String> textPart : m.getTextParts(false)) {
                sb.append(textPart.getContent());
            }
            final StringEntity se = new StringEntity(sb.toString(), HTTP.UTF_8);
            se.setContentType("text/xml");
            httpPost.setHeader("Content-Type","text/xml;charset=UTF-8");
            httpPost.setEntity(se);

            // perform HTTP post
            LOG.info("Performing post to to Autogate");
            final HttpResponse response = httpClient.execute(httpPost);

            // check results
            final HttpEntity httpEntity = response.getEntity();
            String httpEntityString = null;
            if(httpEntity != null) {
                httpEntityString = EntityUtils.toString(httpEntity);
            }
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK ||
                    response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                LOG.info("Lead succesfully posted to Autogate, messageId=" + m.getMessageId()
                        + ", responseCode=" + response.getStatusLine().getStatusCode()
                        + ", response=" + httpEntityString);
            } else {
                LOG.error("Failed to post Lead to Autogate, messageId=" + m.getMessageId()
                        + ", responseCode=" + response.getStatusLine().getStatusCode()
                        + ", requestBody=" + sb.toString()
                        + ", response=" + httpEntityString);
            }
        } catch (Exception e) {
            LOG.error("Failed to deliver mail messageId=" + m.getMessageId(), e);
            throw new MailDeliveryException("Failed to deliver mail messageId=" + m.getMessageId(), e);
        }
    }
}