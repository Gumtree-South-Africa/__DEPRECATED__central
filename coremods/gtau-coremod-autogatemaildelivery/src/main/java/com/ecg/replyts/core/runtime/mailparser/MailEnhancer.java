package com.ecg.replyts.core.runtime.mailparser;

import com.codahale.metrics.Counter;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.dom.field.AddressListField;
import org.apache.james.mime4j.dom.field.FieldName;
import org.apache.james.mime4j.field.Fields;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This is a helper class to amend the To, Cc and Bcc headers with any overrides
 */
@Component
public class MailEnhancer {

    private final static Logger LOG = LoggerFactory.getLogger(MailEnhancer.class);

    @Value("${replyts.header.email.from.list:X-Cust-Email-From-Override}")
    private String emailFromListHeader;
    @Value("${replyts.header.email.to.list:X-Cust-Email-To-Override}")
    private String emailToListHeader;
    @Value("${replyts.header.email.cc.list:X-Cust-Email-Cc-Override}")
    private String emailCcListHeader;
    @Value("${replyts.header.email.bcc.list:X-Cust-Email-Bcc-Override}")
    private String emailBccListHeader;

    private static final Counter BROADCAST_SUCCESS = TimingReports.newCounter("message-box.postBoxBroadcast.success");

    /**
     * Attempt to override the mail headers with To, Cc and Bcc lists
     */
    public Mail process(Mail m) {
        try {
            StructuredMutableMail mail = (StructuredMutableMail) m;
            boolean isUpdated = false;
            isUpdated |= amendMailHeader(mail, FieldName.FROM, emailFromListHeader);
            isUpdated |= amendMailHeader(mail, FieldName.TO, emailToListHeader);
            isUpdated |= amendMailHeader(mail, FieldName.CC, emailCcListHeader);
            isUpdated |= amendMailHeader(mail, FieldName.BCC, emailBccListHeader);

            if(isUpdated) {
                BROADCAST_SUCCESS.inc(); // Update the counter
            }
            LOG.info("Headers for message id {} after processing {} ",mail.getMessageId(), mail.getUniqueHeaders());
            return mail;
        } catch (Exception e) {
            LOG.error("Exception while processing mail", e); // Do nothing
            return m;
        }
    }

    private boolean amendMailHeader(StructuredMutableMail mail, String headerKey, String overrideHeader) {
        String overrideValue = mail.getUniqueHeader(overrideHeader);
        mail.removeHeader(overrideHeader); // Clean up the header even if its empty
        if (!StringUtils.hasText(overrideValue)) {
            return false; // Do nothing when no overrides are found
        }

        // Collect all the addresses and update the header
        setAddressList(mail, headerKey, extractAddresses(overrideValue));
        return true;
    }

    private List<Mailbox> extractAddresses(String val) {
        if (StringUtils.hasText(val)) {
            try {
                InternetAddress[] parsedValues = InternetAddress.parse(val);
                if ((parsedValues != null) && (parsedValues.length != 0)) {
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
        }
        return Collections.emptyList();
    }

    private void setAddressList(StructuredMutableMail mail, String fieldName, Collection<Mailbox> addresses) {
        if (!CollectionUtils.isEmpty(addresses)) {
            AddressListField addressField = Fields.addressList(fieldName, addresses);
            mail.removeHeader(fieldName);
            mail.addHeader(addressField.getName(), addressField.getBody());
        }
    }
}
