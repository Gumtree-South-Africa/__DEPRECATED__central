package com.ecg.comaas.mde.listener.rating;

import com.ecg.replyts.core.api.model.conversation.Message;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Map;
import java.util.Optional;

public final class EmailInviteAssembler {

    private static final String CUSTOM_HEADER_PREFIX = "X-Cust-";
    private static final String HEADER_AD_ID = "X-ADID";
    private static final String HEADER_MOBILE_VI = "X-Mobile-Vi";
    private static final String HEADER_FROM = "From";
    private static final String HEADER_REPLY_TO = "Reply-To";
    private static final String HEADER_CUSTOMER_ID = CUSTOM_HEADER_PREFIX + "Customer_Id";
    private static final String HEADER_IP_ADDRESS = CUSTOM_HEADER_PREFIX + "Ip_Address_V4V6";
    private static final String HEADER_PUBLISHER = CUSTOM_HEADER_PREFIX + "Publisher";
    private static final String HEADER_BUYER_LOCALE = CUSTOM_HEADER_PREFIX + "Buyer_Locale";
    private static final String HEADER_BUYER_DEVICE_ID = CUSTOM_HEADER_PREFIX + "Buyer_Device_Id";
    private static final String HEADER_BUYER_CUSTOMER_ID = CUSTOM_HEADER_PREFIX + "Buyer_Customer_Id";

    private static final Logger logger = LoggerFactory.getLogger(EmailInviteAssembler.class);

    public static final String BY_CONTACT_MESSAGE = "BY_CONTACT_MESSAGE";

    private EmailInviteAssembler() {
        throw new RuntimeException("use static methods");
    }

    static EmailInviteEntity assemble(final Message message, final String conversationId) {
        final EmailInviteEntity emailInviteEntity = new EmailInviteEntity();
        emailInviteEntity.setTriggerType(BY_CONTACT_MESSAGE);

        final Map<String, String> headers = message.getCaseInsensitiveHeaders();
        getLongHeader(HEADER_AD_ID, headers).ifPresent(emailInviteEntity::setAdId);
        getLongHeader(HEADER_CUSTOMER_ID, headers).ifPresent(emailInviteEntity::setDealerId);

        // Check if replyto exists
        Optional<String> replyTo = getStringHeader(HEADER_REPLY_TO, headers);
        if (replyTo.isPresent()) {
            emailInviteEntity.setBuyerEmail(extractEmail(replyTo.get()));
        } else {
            getStringHeader(HEADER_FROM, headers).ifPresent(v -> emailInviteEntity.setBuyerEmail(extractEmail(v)));
        }

        getStringHeader(HEADER_IP_ADDRESS, headers).ifPresent(emailInviteEntity::setIpAddress);
        getStringHeader(HEADER_PUBLISHER, headers).ifPresent(emailInviteEntity::setSource);
        getStringHeader(HEADER_BUYER_LOCALE, headers).ifPresent(emailInviteEntity::setLocale);
        getStringHeader(HEADER_MOBILE_VI, headers).ifPresent(emailInviteEntity::setMobileViId);

        getStringHeader(HEADER_BUYER_DEVICE_ID, headers).ifPresent(emailInviteEntity::setBuyerDeviceId);
        getStringHeader(HEADER_BUYER_CUSTOMER_ID, headers).ifPresent(emailInviteEntity::setBuyerCustomerId);

        emailInviteEntity.setReplytsConversationId(conversationId);

        return emailInviteEntity;
    }


    private static String extractEmail(final String fullEmail) {
        try {
            return new InternetAddress(fullEmail).getAddress();
        } catch (AddressException e) {
            logger.info("Problems extracting email address from '{}', but will try a workaround. Details: {}", fullEmail, e.getMessage());
            return extractEmailFallback(fullEmail)
                    .orElseThrow(() -> new RuntimeException("Invalid recipient address " + fullEmail));
        }
    }

    private static Optional<String> extractEmailFallback(final String fullEmail) {
        // sometimes internet address fails if special characters are in the personal part
        final int beginMailAddress = fullEmail.lastIndexOf("<");
        final int endMailAddress = fullEmail.lastIndexOf(">");

        if (beginMailAddress < endMailAddress && beginMailAddress > -1) {
            try {
                return Optional.of(new InternetAddress(fullEmail.substring(beginMailAddress + 1, endMailAddress)).getAddress());
            } catch (AddressException e) {
                logger.error("Error extracting email address from '{}', Details: {}", fullEmail, e.getMessage(), e);
            }
        }

        return Optional.empty();
    }

    private static Optional<Long> getLongHeader(final String headerName, final Map<String, String> headers) {
        if (headers.containsKey(headerName)) {
            final String value = headers.get(headerName)
                    .replaceFirst("COMA", "");
            if (StringUtils.isNumeric(value)) {
                return Optional.of(new Long(value));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> getStringHeader(String headerName, Map<String, String> headers) {
        return Optional.ofNullable(headers.getOrDefault(headerName, null));
    }

}
