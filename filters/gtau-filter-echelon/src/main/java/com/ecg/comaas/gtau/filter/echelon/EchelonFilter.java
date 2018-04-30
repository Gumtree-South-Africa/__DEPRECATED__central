package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Strings;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import static com.ecg.replyts.core.runtime.prometheus.PrometheusFailureHandler.reportExternalServiceFailure;

public class EchelonFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(EchelonFilter.class);
    private static final String IP_CUSTOM_HEADER = "ip";
    private static final String MACHINE_ID_CUSTOM_HEADER = "mach-id";
    private static final String CATEGORY_ID_CUSTOM_HEADER = "categoryid";
    private static final String KO = "KO";
    private static final String ECHELON_DESCRIPTION = "echelon_filter";

    private final EchelonFilterConfiguration configuration;
    private final CloseableHttpClient httpClient;

    public EchelonFilter(EchelonFilterConfiguration configuration, CloseableHttpClient httpClient) {
        LOG.info("Creating new instance of EchelonFilter: {} with configuration '{}'", hashCode(), configuration);
        this.configuration = configuration;
        this.httpClient = httpClient;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        if (MessageDirection.BUYER_TO_SELLER != context.getMessageDirection()
                || context.getConversation() == null
                || context.getConversation().getMessages() == null
                || Strings.isNullOrEmpty(context.getConversation().getCustomValues().get(IP_CUSTOM_HEADER))) {
            return Collections.emptyList();
        }

        return doGet(buildUrl(context.getConversation(), configuration.getEndpointUrl()));
    }

    private List<FilterFeedback> doGet(String url) {
        HttpGet httpGet = new HttpGet(url);
        LOG.trace("Sending echelon request [{}]", url);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            if (HttpStatus.SC_OK != response.getStatusLine().getStatusCode()) {
                reportExternalServiceFailure(ECHELON_DESCRIPTION);
                LOG.error("Got {} status code instead of 200", response.getStatusLine().getStatusCode());
                return Collections.emptyList();
            }

            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                String body = EntityUtils.toString(httpEntity).trim();
                if (!Strings.isNullOrEmpty(body) && body.startsWith(KO)) {
                    return Collections.singletonList(new FilterFeedback("Echelon", extractKoFromBody(body), configuration.getScore(), FilterResultState.DROPPED));
                }
            }
        } catch (Exception e) {
            reportExternalServiceFailure(ECHELON_DESCRIPTION);
            LOG.error("HTTP GET to {} failed", url, e);
        }
        return Collections.emptyList();
    }

    private static String buildUrl(Conversation conversation, String endpointUrl) {
        String ip = conversation.getCustomValues().get(IP_CUSTOM_HEADER);
        String machineId = conversation.getCustomValues().get(MACHINE_ID_CUSTOM_HEADER);
        if (machineId != null && machineId.equals("unknown")) {
            machineId = "";
        }
        String categoryId = conversation.getCustomValues().get(CATEGORY_ID_CUSTOM_HEADER);
        String adId = conversation.getAdId();
        String email = conversation.getBuyerId();

        return endpointUrl + '?' +
                "adId=" + encode(adId) + '&' +
                "ip=" + encode(ip) + '&' +
                "machineId=" + encode(machineId) + '&' +
                "categoryId=" + encode(categoryId) + '&' +
                "email=" + encode(email);
    }

    private static String encode(String str) {
        if (Strings.isNullOrEmpty(str)) {
            return "";
        }
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Failed to encode {} in UTF-8", str, e);
            return "";
        }
    }

    private static String extractKoFromBody(String body) {
        return body.substring(KO.length()).trim();
    }
}
