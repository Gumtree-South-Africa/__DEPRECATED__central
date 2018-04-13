package com.ecg.comaas.core.filter.ebayservices;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;

import java.util.Optional;
import java.util.regex.Pattern;

public class IpAddressExtractor {

    public static final String IP_ADDR_HEADER = "X-Cust-Ip";

    private static final Pattern IP_V4_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");

    public Optional<String> retrieveIpAddress(MessageProcessingContext mpc) {
        return isFirstMessage(mpc) && containsIpV4(mpc) ?
                Optional.of(mpc.getMail().get().getUniqueHeader(IP_ADDR_HEADER)) :
                Optional.empty();
    }

    private boolean containsIpV4(MessageProcessingContext mpc) {
        String ipAddressHeader = mpc.getMail().get().getUniqueHeader(IP_ADDR_HEADER);
        if(ipAddressHeader == null) {
            return false;
        }

        return IP_V4_PATTERN.matcher(ipAddressHeader).find();
    }

    private boolean isFirstMessage(MessageProcessingContext mpc) {
        return mpc.getConversation().getMessages().size() == 1;
    }
}
