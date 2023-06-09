package com.ecg.comaas.mde.postprocessor.sms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.Map.Entry;


public class ContactMessageSmsService {
    private static final Logger LOG = LoggerFactory.getLogger(ContactMessageSmsService.class);
    private static Map<String, String> ABBREVIATIONS = new HashMap<>();
    private static Map<String, String> SENDER_LOCALE = new HashMap<>();

    static final String SENDER = "Sender";

    static {
        ABBREVIATIONS.put(quote("autohaus"), "AH");
        ABBREVIATIONS.put(quote("fahrzeug"), "Fzg.");
        ABBREVIATIONS.put(quote("händler"), "Hdl.");
        ABBREVIATIONS.put(quote("mercedes-benz"), "MB");
        ABBREVIATIONS.put(quote("niederlassung"), "Ndl.");
        ABBREVIATIONS.put(quote("volkswagen"), "VW");

        SENDER_LOCALE.put("cs", SENDER);
        SENDER_LOCALE.put("cz", SENDER);
        SENDER_LOCALE.put("en", SENDER);
        SENDER_LOCALE.put("ru", SENDER);
        SENDER_LOCALE.put("de", "Abs.");
        SENDER_LOCALE.put("es", "Remitente");
        SENDER_LOCALE.put("fr", "Expéditeur");
        SENDER_LOCALE.put("it", "Mittente");
        SENDER_LOCALE.put("pl", "Wysyłający");
        SENDER_LOCALE.put("ro", "Expeditor");
    }

    private static String quote(String word) {
        return "(?i)\\Q" + word + "\\E";
    }

    private final static int SMS_MAX_LENGTH = 160;

    private ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient;
    private String apiUrl;

    public ContactMessageSmsService(HttpClient httpClient, String apiUrl) {
        this.httpClient = httpClient;
        this.apiUrl = apiUrl;
    }

    public boolean send(ContactMessage contactMessage) throws Exception {
        if (StringUtils.isEmpty(apiUrl)) {
            LOG.warn("Not sending sms messages, apiUrl not configured.");
            return false;
        }

        String message = createMessage(contactMessage);

        String phoneNumber = contactMessage.getSmsPhoneNumber();
        SmsSendRequest smsSendRequest = new SmsSendRequest(phoneNumber, message, "Comaas contact message");

        HttpPost request = post(contactMessage.getCustomerId(), smsSendRequest);
        return httpClient.execute(request, SuccessStatusCodeResponseHandler.INSTANCE);
    }

    private HttpPost post(Long dealerId, SmsSendRequest payload) throws Exception {
        String payloadString = objectMapper.writeValueAsString(payload);
        StringEntity params = new StringEntity(payloadString, Charset.forName("UTF-8"));

        String targetUrl = apiUrl + dealerId;
        LOG.debug("Sending SMS to '{}' and payload '{}'", targetUrl, payloadString);

        HttpPost post = new HttpPost(targetUrl);
        post.addHeader(HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        post.addHeader(HTTP.CONTENT_ENCODING, "UTF-8");
        post.setEntity(params);
        return post;
    }

    private enum SuccessStatusCodeResponseHandler implements ResponseHandler<Boolean> {
        INSTANCE;

        @Override
        public Boolean handleResponse(HttpResponse response) throws IOException {
            try {
                StatusLine statusLine = response.getStatusLine();

                if (statusLine == null) {
                    LOG.warn("Invalid Response statusLine null");
                    return false;
                }
                if (200 <= statusLine.getStatusCode() && statusLine.getStatusCode() < 400) {
                    return true;
                }
                LOG.warn("Failed response status code {} and response {}", statusLine.getStatusCode(), entityAsText(response));
                return false;
            } finally {
                if (response.getEntity() != null) {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        }

        private static String entityAsText(HttpResponse response) {
            return Optional.ofNullable(response.getEntity()).map(entity -> {
                try {
                    return EntityUtils.toString(entity);
                } catch (IOException exceptionToSkip) {
                    return org.apache.commons.lang3.StringUtils.EMPTY;
                }
            }).orElse(org.apache.commons.lang3.StringUtils.EMPTY);
        }
    }

    private String createMessage(ContactMessage contactMessage) throws IOException {

        Locale sellerLocale = parse(contactMessage.getSellerLocale());

        StringBuilder sms = new StringBuilder();
        sms.append(parseSender(contactMessage.getSellerLocale())).append(": ");
        sms.append(cutBuyerDisplayName(contactMessage.getDisplayName()));
        sms.append(" - ");
        if (hasBuyerPhoneNumber(contactMessage.getBuyerPhoneNumber())) {
            sms.append(contactMessage.getBuyerPhoneNumber().getDisplayNumber());
        } else {
            sms.append(contactMessage.getBuyerMailAddress());
        }
        sms.append(" - ");
        sms.append(getVehicleDescription(contactMessage, sellerLocale)).append("; ");
        if (StringUtils.hasText(contactMessage.getInternalNumber())) {
            sms.append(contactMessage.getInternalNumber());
        } else {
            String displayPrice = getDisplayPrice(contactMessage, sellerLocale);
            if (displayPrice != null) {
                sms.append(displayPrice);
            }
        }
        sms.append("; ");

        String strSms = sms.toString();
        /*
         * last character is a "
         */
        if (strSms.length() > SMS_MAX_LENGTH - 1) {
            strSms = strSms.substring(0, SMS_MAX_LENGTH - 5) + "...";
        }
        return strSms;
    }

    private boolean hasBuyerPhoneNumber(PhoneNumber phoneNumber) {
        return phoneNumber != null && StringUtils.hasText(phoneNumber.getDisplayNumber());
    }


    private String cutBuyerDisplayName(String displayName) {
        if (displayName == null) {
            return "";
        }

        for (Entry<String, String> entry : ABBREVIATIONS.entrySet()) {
            displayName = displayName.replaceAll(entry.getKey(), entry.getValue());
        }


        if (displayName.length() <= 20) {
            return displayName;
        }

        return displayName.substring(0, 20);
    }


    private String getVehicleDescription(ContactMessage contactMessage, Locale locale) {

        String asString = contactMessage.getMakeName();
        if (asString.length() > 10) {
            asString = asString.substring(0, 10);
        }

        String modelDescription = contactMessage.getModelDescription();
        if (modelDescription != null && modelDescription.length() > 30) {
            modelDescription = modelDescription.substring(0, 27) + "...";
        }

        if (modelDescription != null) {
            asString = asString + " " + modelDescription;
        }

        return asString;
    }


    // ad.getPriceInMainUnit()<=0 || ad.getCurrency()
    private String getDisplayPrice(ContactMessage contactMessage, Locale locale) {
        if (contactMessage.getPriceInMainUnit() <= 0 || contactMessage.getCurrency() == null) {
            return null;
        }

        Currency currency = Currency.getInstance(contactMessage.getCurrency());

        return contactMessage.getPriceInMainUnit() + " " + currency.getSymbol(locale);
    }

    private static Locale parse(String string) {
        String[] parts = string.split("_");
        if (parts.length < 2) {
            return new Locale(parts[0]);
        }

        return new Locale(parts[0], parts[1]);
    }

    static String parseSender(String string) {
        return Optional.ofNullable(string)
                .map(s -> s.split("_"))
                .filter(parts -> parts.length > 0)
                .map(parts -> parts[0])
                .map(String::toLowerCase)
                .map(SENDER_LOCALE::get)
                .orElse(SENDER);
    }
}