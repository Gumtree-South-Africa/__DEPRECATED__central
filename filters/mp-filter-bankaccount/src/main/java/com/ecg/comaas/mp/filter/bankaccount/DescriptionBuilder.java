package com.ecg.comaas.mp.filter.bankaccount;

import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import java.util.Arrays;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

@Component
class DescriptionBuilder {
    private MailCloakingService mailCloakingService;

    @Autowired
    public DescriptionBuilder(MailCloakingService mailCloakingService) {
        this.mailCloakingService = mailCloakingService;
    }

    public String build(Conversation conv, BankAccountMatch match, Message message, int mailMatchCount) {
        String buyerUserId = trimToEmpty(conv.getCustomValues().get("from-userid"));
        String sellerUserId = trimToEmpty(conv.getCustomValues().get("to-userid"));
        boolean isFromBuyerToSeller = message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER;
        String fraudsterUserConversationEmail = isFromBuyerToSeller ? conv.getBuyerId() : conv.getSellerId();
        String fraudsterAnonEmail = mailCloakingService.createdCloakedMailAddress(message.getMessageDirection().getFromRole(), conv).getAddress();
        String fraudsterActualEmail = trimToEmpty(getActualSenderEmailAddress(message));
        String fraudsterUserId = isFromBuyerToSeller ? buyerUserId : sellerUserId;
        String victimEmail = isFromBuyerToSeller ? conv.getSellerId() : conv.getBuyerId();
        String victimUserId = isFromBuyerToSeller ? sellerUserId : buyerUserId;

        return StringUtils.join(Arrays.asList(
                fraudsterUserConversationEmail,
                String.valueOf(match.getScore()),
                match.getBankAccount(),
                fraudsterAnonEmail,
                fraudsterActualEmail,
                trimToEmpty(getIpFromMessage(message)),
                victimEmail,
                String.valueOf(conv.getId()),
                String.valueOf(mailMatchCount),
                fraudsterUserId,
                victimUserId
        ), "|");
    }

    private String getActualSenderEmailAddress(Message message) {
        String replyTo = getEmailHeader(message, Mail.REPLY_TO);
        String from = getEmailHeader(message, Mail.FROM);
        return Stream.of(replyTo, from)
                .filter(emailAddress -> emailAddress != null && !emailAddress.endsWith("@marktplaats.nl"))
                .collect(joining(", "));
    }

    private String getEmailHeader(Message message, String senderHeader) {
        String value = message.getCaseInsensitiveHeaders().get(senderHeader);
        try {
            return value == null ? null : new InternetAddress(value).getAddress();
        } catch (AddressException e) {
            return value.replaceAll("[^a-zA-Z0-9@! #$%&'*+-/=?^_`{}~.]", "");
        }
    }

    private String getIpFromMessage(Message message) {
        String ip = BankAccountFilterUtil.coalesce(
                trimToNull(message.getCaseInsensitiveHeaders().get("From-Ip")), // "From-Ip" - Mail.FROM_IP
                trimToNull(message.getCaseInsensitiveHeaders().get("X-Originating-Ip")), // "X-Originating-Ip" - Mail.X_ORIGINATING_IP
                trimToNull(message.getCaseInsensitiveHeaders().get("X-Sourceip")), // "X-SourceIP" - Mail.X_SOURCEIP
                trimToNull(message.getCaseInsensitiveHeaders().get("X-Aol-Ip"))); // "X-AOL-IP" - Mail.X_AOL_IP
        return StringUtils.strip(ip, "[]");
    }
}
