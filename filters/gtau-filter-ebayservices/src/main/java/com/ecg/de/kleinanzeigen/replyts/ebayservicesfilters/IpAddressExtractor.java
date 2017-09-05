package com.ecg.de.kleinanzeigen.replyts.ebayservicesfilters;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;

import java.util.Optional;
import java.util.regex.Pattern;

public class IpAddressExtractor {

    public static final String IP_ADDR_HEADER = "X-Cust-Ip";

    private static final Pattern IP_V4_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$");


    public Optional<String> retrieveIpAddress(MessageProcessingContext mpc) {
        return containsIpV4(mpc) ?
                Optional.of(mpc.getMail().getUniqueHeader(IP_ADDR_HEADER)) :
                Optional.empty();
    }

    private boolean containsIpV4(MessageProcessingContext mpc) {
        String ipAddressHeader = mpc.getMail().getUniqueHeader(IP_ADDR_HEADER);
        if(ipAddressHeader == null) {
            return false;
        }

        return IP_V4_PATTERN.matcher(ipAddressHeader).find();

    }
}
