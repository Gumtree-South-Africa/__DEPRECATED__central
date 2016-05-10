package com.ebay.replyts.australia.echelon;

import com.ebay.replyts.australia.echelon.config.EchelonFilterConfiguration;
import com.ebay.replyts.australia.echelon.feedback.EchelonFeedback;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mdarapour
 */
public class EchelonFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(EchelonFilter.class);

    static final String IP_CUSTOM_HEADER            = "ip";         // X-CUST-IP
    static final String MACHINE_ID_CUSTOM_HEADER    = "mach-id";    // X-CUST-MACH-ID
    static final String CATEGORY_ID_CUSTOM_HEADER   = "categoryid"; // X-CUST-CATEGORYID

    private static final String KO = "KO";
    private String endpointUrl;
    private int endpointTimeout;
    private int score;

    public EchelonFilter(EchelonFilterConfiguration config) {
        super();
        LOG.info("Creating new instance of EchelonFilter: " + hashCode());
        endpointUrl = config.getEndpointUrl();
        endpointTimeout = config.getEndpointTimeout();
        score = config.getScore();
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        final ArrayList<FilterFeedback> resp = new ArrayList<FilterFeedback>();

        if (MessageDirection.BUYER_TO_SELLER == context.getMessageDirection()) {
            Conversation c = context.getConversation();

            try {
                if ((null != c) && (null != c.getMessages())) {
                    String ip = c.getCustomValues().get(IP_CUSTOM_HEADER);

                    if(Strings.isNullOrEmpty(ip)) {
                        return resp;
                    }

                    String machineId = c.getCustomValues().get(MACHINE_ID_CUSTOM_HEADER);

                    final String categoryId = c.getCustomValues().get(CATEGORY_ID_CUSTOM_HEADER);
                    final String adId = c.getAdId();
                    final String email = c.getBuyerId();
                    final StringBuilder builder = new StringBuilder();

                    if (machineId != null && machineId.equals("unknown")) {
                        machineId = "";
                    }

                    builder.append(endpointUrl);
                    builder.append('?');
                    builder.append("adId=").append(encode(adId)).append('&');
                    builder.append("ip=").append(encode(ip)).append('&');
                    builder.append("machineId=").append(encode(machineId)).append('&');
                    builder.append("categoryId=").append(encode(categoryId)).append('&');
                    builder.append("email=").append(encode(email));

                    final String notifyUrl = builder.toString();
                    final URL url = new URL(notifyUrl);
                    final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                    try{
                        connection.setRequestMethod("GET");
                        connection.setReadTimeout(endpointTimeout);
                        LOG.debug("Sending echelon request [{}]",connection.getURL());
                        connection.connect();
                        final int responseCode = connection.getResponseCode();
                        if (HttpURLConnection.HTTP_OK == responseCode) {
                            final BufferedReader is = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            final String firstLine = is.readLine();
                            final StringBuilder sb = new StringBuilder();
                            String line;
                            while (null != (line = is.readLine())) {
                                sb.append(line);
                                sb.append('\n');
                            }
                            is.close();

                            if ((null != firstLine) && firstLine.trim().equals(KO)) {
                                final EchelonFeedback feedback = new EchelonFeedback(sb.toString(), score);
                                resp.add(feedback);
                            }
                        } else {
                            LOG.error("Didn't get a 200 response from endpoint, but a " + responseCode);
                        }
                    }catch(IOException ex){
                        LOG.error("Error notifying " + notifyUrl, ex);
                    }finally {
                        connection.disconnect();
                    }
                }
            }catch(Exception ex) {
                LOG.error("Couldn't open connection to endpoint" + endpointUrl, ex);
            }
        }

        return resp;
    }

    private String encode(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("Panic!", e);
            return "";
        }
    }
}
