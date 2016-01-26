package com.ecg.replyts.core.runtime.mailcloaking;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AnonymizedMailConverter {

    private final String mailFormatTemplate;

    private final String buyerRole;
    private final String sellerRole;
    private final String[] domains;
    //private final Pattern mailFormatPattern;
    private final Map<String, Pattern> mailFormatPatterns;

    @Autowired
    public AnonymizedMailConverter(
            @Value("${mailcloaking.localized.buyer}") String buyerRole,
            @Value("${mailcloaking.localized.seller}") String sellerRole,
            @Value("${mailcloaking.domains}") String[] domains,
            @Value("${mailcloaking.seperator:.}") String mailCloakingSeperator

    ) {

        Preconditions.checkArgument(mailCloakingSeperator.length() == 1, "Mail cloacking seperator size must be a single character.");

        this.buyerRole = buyerRole;
        this.sellerRole = sellerRole;
        this.domains = domains.clone();
        this.mailFormatTemplate = "%s" + mailCloakingSeperator + "%s@%s";

        String quotedSep = Pattern.quote(mailCloakingSeperator);

        ImmutableMap.Builder<String, Pattern> builder = ImmutableMap.builder();
        for (String domain : domains) {
            String platformDomain = domain.replaceAll("\\.", "\\."); // replace all dots with escaped dots for regex
            String pattern = String.format("(%s|%s)" + quotedSep + "(-?[a-z0-9]+)@%s", buyerRole, sellerRole, platformDomain);
            Pattern mailFormatPattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            builder.put(domain, mailFormatPattern);
        }

        this.mailFormatPatterns = builder.build();
    }

    public MailAddress fromSecretToMail(Conversation conv, ConversationRole role) {
        String roleName = ConversationRole.Buyer == role ? buyerRole : sellerRole;
        String hash = conv.getSecretFor(role);
        String domain = renderDomainFromContext(conv, role);
        String ma = String.format(mailFormatTemplate, roleName, hash, domain);
        return new MailAddress(ma);
    }

    public String renderDomainFromContext(Conversation conv, ConversationRole role) {
        final String headerKey = ConversationRole.Buyer.equals(role) ? "buyer_domain" : "seller_domain";
        final Map<String, String> customHeaders = conv.getCustomValues();
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

    public String fromMailToSecret(MailAddress mailAddress) {
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
