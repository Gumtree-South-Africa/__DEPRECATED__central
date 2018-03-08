package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

import java.util.Optional;
import java.util.regex.Pattern;

public class IpAddressExtractor {

    public static final String IP_ADDR_HEADER = "X-Cust-Ip";

    private static final Pattern IP_V4_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");


    public Optional<String> retrieveIpAddress(MessageProcessingContext context) {
        return containsIpV4(context) ?
                Optional.of(context.getMail().get().getUniqueHeader(IP_ADDR_HEADER)) :
                Optional.empty();
    }

    private boolean containsIpV4(MessageProcessingContext mpc) {
        Optional<Mail> mail = mpc.getMail();
        if (!mail.isPresent()) {
            return false;
        }
        String ipAddressHeader = mail.get().getUniqueHeader(IP_ADDR_HEADER);
        if(ipAddressHeader == null) {
            return false;
        }

        return IP_V4_PATTERN.matcher(ipAddressHeader).find();

    }
}
