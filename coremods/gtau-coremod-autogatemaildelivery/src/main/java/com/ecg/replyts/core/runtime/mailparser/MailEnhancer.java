package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

/**
 * This is a helper class to amend the To, Cc and Bcc headers with any overrides
 */
@ComaasPlugin
@Component
@Profile(TENANT_GTAU)
public class MailEnhancer {

    private static final Logger LOG = LoggerFactory.getLogger(MailEnhancer.class);

    private final String emailFromListHeader;
    private final String emailToListHeader;
    private final String emailCcListHeader;
    private final String emailBccListHeader;

    @Autowired
    public MailEnhancer(
            @Value("${replyts.header.email.from.list:X-Cust-Email-From-Override}") String emailFromListHeader,
            @Value("${replyts.header.email.to.list:X-Cust-Email-To-Override}") String emailToListHeader,
            @Value("${replyts.header.email.cc.list:X-Cust-Email-Cc-Override}") String emailCcListHeader,
            @Value("${replyts.header.email.bcc.list:X-Cust-Email-Bcc-Override}") String emailBccListHeader
    ) {
        this.emailFromListHeader = emailFromListHeader;
        this.emailToListHeader = emailToListHeader;
        this.emailCcListHeader = emailCcListHeader;
        this.emailBccListHeader = emailBccListHeader;
    }

    /**
     * Attempt to override the mail headers with To, Cc and Bcc lists
     */
    public Mail process(Mail obtainedMail) {
        try {
            StructuredMutableMail mail = (StructuredMutableMail) obtainedMail;

            amendMailHeader(mail, FieldName.FROM, emailFromListHeader);
            amendMailHeader(mail, FieldName.TO, emailToListHeader);
            amendMailHeader(mail, FieldName.CC, emailCcListHeader);
            amendMailHeader(mail, FieldName.BCC, emailBccListHeader);

            LOG.info("Headers for message id {} after processing {} ", mail.getMessageId(), mail.getUniqueHeaders());
            return mail;
        } catch (Exception e) {
            LOG.error("Exception while processing mail", e); // Do nothing
            return obtainedMail;
        }
    }

    private static void amendMailHeader(StructuredMutableMail mail, String headerKey, String overrideHeader) {
        String overrideValue = mail.getUniqueHeader(overrideHeader);
        mail.removeHeader(overrideHeader); // Clean up the header even if its empty
        if (StringUtils.hasText(overrideValue)) {
            setAddressList(mail, headerKey, extractAddresses(overrideValue));
        }
    }

    private static List<Mailbox> extractAddresses(String val) {
        try {
            InternetAddress[] parsedValues = InternetAddress.parse(val);
            if (parsedValues.length != 0) {
                List<Mailbox> addressList = new LinkedList<>();
                for (InternetAddress address : parsedValues) {
                    Mailbox mailbox = AddressBuilder.DEFAULT.parseMailbox(address.toString());
                    addressList.add(mailbox);
                }
                return addressList;
            }
        } catch (AddressException | ParseException e) {
            LOG.error("Exception while processing addresses" + val, e); // Do nothing
        }
        return Collections.emptyList();
    }

    private static void setAddressList(StructuredMutableMail mail, String fieldName, Collection<Mailbox> addresses) {
        if (!CollectionUtils.isEmpty(addresses)) {
            AddressListField addressField = Fields.addressList(fieldName, addresses);
            mail.removeHeader(fieldName);
            mail.addHeader(addressField.getName(), addressField.getBody());
        }
    }
}