package com.ecg.replyts.core.runtime.mailcloaking;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AnonymizedMailConverter {
    private static final char MAIL_CLOAKING_SEPARATOR = '-'; // Do not ever change this, you will break replies to existing messages.

    private final String buyerRole;
    private final String sellerRole;
    private final String[] domains;
    private final Map<String, Pattern> mailFormatPatterns;

    @Autowired
    public AnonymizedMailConverter(
            @Value("${mailcloaking.localized.buyer}") String buyerRole,
            @Value("${mailcloaking.localized.seller}") String sellerRole,
            @Value("${mailcloaking.domains}") String[] domains) {
        this.buyerRole = buyerRole;
        this.sellerRole = sellerRole;
        this.domains = domains.clone();

        ImmutableMap.Builder<String, Pattern> builder = ImmutableMap.builder();
        for (String domain : domains) {
            String platformDomain = domain.replaceAll("\\.", "\\."); // replace all dots with escaped dots for regex
            String pattern = String.format("(%s|%s).(-?[a-z0-9]+)@%s", buyerRole, sellerRole, platformDomain);
            Pattern mailFormatPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            builder.put(domain, mailFormatPattern);
        }

        this.mailFormatPatterns = builder.build();
    }

    MailAddress toCloakedEmailAddress(Conversation conv, ConversationRole role) {
        String ma = toCloakedEmailAddress(conv.getSecretFor(role), role, conv.getCustomValues());

        return new MailAddress(ma);
    }

    public String toCloakedEmailAddress(String secret, ConversationRole role, Map<String, String> metaData) {
        String roleName = ConversationRole.Buyer == role ? buyerRole : sellerRole;
        String domain = renderDomainFromContext(role, metaData);
        return String.format("%s%s%s@%s", roleName, MAIL_CLOAKING_SEPARATOR, secret, domain);
    }

    private String renderDomainFromContext(ConversationRole role, Map<String, String> customHeaders) {
        final String headerKey = ConversationRole.Buyer.equals(role) ? "buyer_domain" : "seller_domain";
        if (!customHeaders.containsKey(headerKey)) {
            return this.domains[0];
        }

        //check that domain is configured, if not use default
        final String domain = customHeaders.get(headerKey);
        if (!this.mailFormatPatterns.containsKey(domain)) {
            return this.domains[0];
        }

        return domain;
    }

    String toParticipantSecret(MailAddress mailAddress) {
        for (String domain : this.domains) {
            Pattern mailFormatPattern = this.mailFormatPatterns.get(domain);
            Matcher matcher = mailFormatPattern.matcher(mailAddress.getAddress());
            if (matcher.matches()) {
                return matcher.group(2);
            }
        }

        throw new IllegalArgumentException("Mail Address " + mailAddress.getAddress() + " is not in the correct format");
    }

    public boolean isCloaked(MailAddress mailAddress) {
        for (String domain : this.domains) {
            Pattern mailFormatPattern = this.mailFormatPatterns.get(domain);
            Matcher matcher = mailFormatPattern.matcher(mailAddress.getAddress());
            if (matcher.matches()) {
                return true;
            }
        }

        return false;
    }
}
