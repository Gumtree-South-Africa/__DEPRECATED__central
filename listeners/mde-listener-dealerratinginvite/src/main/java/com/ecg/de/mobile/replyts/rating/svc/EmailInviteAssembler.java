package com.ecg.de.mobile.replyts.rating.svc;

import com.ecg.replyts.core.api.model.conversation.Message;

import de.mobile.dealer.rating.invite.EmailInviteEntity;
import de.mobile.dealer.rating.invite.EmailInviteTriggerType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import java.util.Map;
import java.util.Optional;

/**
 * Created by vbalaramiah on 4/23/15.
 */
public class EmailInviteAssembler {

    private static final String CUSTOM_HEADER_PREFIX = "X-Cust-";

    private static final String HEADER_AD_ID = "X-ADID";

    private static final String HEADER_MOBILE_VI = "X-Mobile-Vi";

    static final String HEADER_FROM = "From";

    static final String HEADER_REPLY_TO = "Reply-To";

    static final String HEADER_CUSTOMER_ID = CUSTOM_HEADER_PREFIX + "Customer_Id";

    static final String HEADER_IP_ADDRESS = CUSTOM_HEADER_PREFIX + "Ip_Address_V4V6";

    static final String HEADER_PUBLISHER = CUSTOM_HEADER_PREFIX + "Publisher";

    static final String HEADER_BUYER_LOCALE = CUSTOM_HEADER_PREFIX + "Buyer_Locale";

    static final String HEADER_BUYER_DEVICE_ID = CUSTOM_HEADER_PREFIX + "Buyer_Device_Id";

    static final String HEADER_BUYER_CUSTOMER_ID = CUSTOM_HEADER_PREFIX + "Buyer_Customer_Id";


    private static final Logger logger = LoggerFactory.getLogger(EmailInviteAssembler.class);

    public EmailInviteEntity toEmailInvite(Message message, String conversationId) {
        final EmailInviteEntity emailInviteEntity = new EmailInviteEntity();
        emailInviteEntity.setTriggerType(EmailInviteTriggerType.BY_CONTACT_MESSAGE);
        
        Map<String, String> headers = message.getHeaders();
        getLongHeader(HEADER_AD_ID, headers).ifPresent(v -> emailInviteEntity.setAdId(v));
        getLongHeader(HEADER_CUSTOMER_ID, headers).ifPresent(v -> emailInviteEntity.setDealerId(v));

        // Check if replyto exists
        Optional<String> replyTo = getStringHeader(HEADER_REPLY_TO, headers);
        if(replyTo.isPresent()) {
            emailInviteEntity.setBuyerEmail(extractEmail(replyTo.get()));
        }else {
            getStringHeader(HEADER_FROM, headers).ifPresent(v -> emailInviteEntity.setBuyerEmail(extractEmail(v)));
        }

        getStringHeader(HEADER_IP_ADDRESS, headers).ifPresent(v -> emailInviteEntity.setIpAddress(v));
        getStringHeader(HEADER_PUBLISHER, headers).ifPresent(v -> emailInviteEntity.setSource(v));
        getStringHeader(HEADER_BUYER_LOCALE, headers).ifPresent(v -> emailInviteEntity.setLocale(v));
        getStringHeader(HEADER_MOBILE_VI, headers).ifPresent(v -> emailInviteEntity.setMobileViId(v));

        getStringHeader(HEADER_BUYER_DEVICE_ID, headers).ifPresent(v -> emailInviteEntity.setBuyerDeviceId(v));
        getStringHeader(HEADER_BUYER_CUSTOMER_ID, headers).ifPresent(v -> emailInviteEntity.setBuyerCustomerId(v));

        emailInviteEntity.setReplytsConversationId(conversationId);
        
        return emailInviteEntity;
    }


    private static String extractEmail(String fullEmail) {
        try {
            InternetAddress email = new InternetAddress(fullEmail);
            return email.getAddress();
        } catch (AddressException e) {
            logger.error("Error extracting email address from , " + fullEmail + ": " + e.getMessage());

            Optional<String> mailAddress = extractEmailFallback(fullEmail);

            return mailAddress.orElseThrow(() -> new RuntimeException("Invalid recpient address " + fullEmail));
        }
    }

    private static Optional<String> extractEmailFallback(String fullEmail)  {

        // sometimes internet address fails if special characters are in the personal part
        int beginMailAddress = fullEmail.lastIndexOf("<");
        int endMailAddress = fullEmail.lastIndexOf(">");

        if(beginMailAddress < endMailAddress && beginMailAddress > -1) {
            try {
                return Optional.of(new InternetAddress(fullEmail.substring(beginMailAddress+1, endMailAddress)).getAddress());
            } catch (AddressException e) {
                logger.error("Error extracting email address from , " + fullEmail + ": " + e.getMessage());
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    private Optional<Long> getLongHeader(String headerName, Map<String, String> headers) {
        if (headers.containsKey(headerName)) {
            String val = headers.get(headerName).replaceFirst("COMA", "");
            if (StringUtils.isNumeric(val)) {
                return Optional.of(new Long(val));
            }
        }
        return Optional.empty();
    }

    private Optional<String> getStringHeader(String headerName, Map<String, String> headers) {
        return Optional.ofNullable(headers.getOrDefault(headerName, null));
    }
    
}
