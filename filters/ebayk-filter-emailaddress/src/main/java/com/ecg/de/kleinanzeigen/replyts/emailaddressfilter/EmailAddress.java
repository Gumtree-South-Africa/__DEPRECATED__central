package com.ecg.de.kleinanzeigen.replyts.emailaddressfilter;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.List;

import static java.lang.String.format;

public class EmailAddress {

    private final String namePart;

    private final InternetAddress address;

    public EmailAddress(String emailAddress) {
        try {
            address = new InternetAddress(emailAddress, true);
            address.validate();

            List<String> parts = Splitter.on('@').splitToList(address.getAddress());
            Preconditions.checkState(parts.size() == 2);

            namePart = parts.get(0);
            String domainPart = parts.get(1);
            if (!domainPart.contains(".")) {
                throw new RuntimeException("Expect '.' in domain part");
            }
        } catch (AddressException e) {
            throw new RuntimeException(format("Not a valid email address: '%s'", emailAddress), e);
        }
    }

    public String getNamePart() {
        return namePart;
    }

    public String getComplete() {
        return address.getAddress();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        EmailAddress that = (EmailAddress) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }
}
