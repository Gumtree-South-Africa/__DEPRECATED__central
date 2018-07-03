package com.ecg.comaas.core.filter.ebayservices;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.util.Optional;

public final class IpAddressExtractor {

    private static final String IP_ADDR_HEADER = "X-Cust-Ip";

    private IpAddressExtractor() {
    }

    public static Optional<String> retrieveIpAddress(MessageProcessingContext mpc) {
        Optional<Mail> mail = mpc.getMail();
        if (mail.isPresent() && isFirstMessage(mpc)) {
            String ipAddressHeader = mail.get().getUniqueHeader(IP_ADDR_HEADER);
            if (InetAddressValidator.getInstance().isValidInet4Address(ipAddressHeader)) {
                return Optional.of(ipAddressHeader);
            }
        }
        return Optional.empty();
    }

    private static boolean isFirstMessage(MessageProcessingContext mpc) {
        return mpc.getConversation().getMessages().size() == 1;
    }
}
