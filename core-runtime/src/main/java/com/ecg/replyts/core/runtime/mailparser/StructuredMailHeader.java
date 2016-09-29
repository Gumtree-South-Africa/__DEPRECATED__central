package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.DecodeMonitor;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.dom.address.Address;
import org.apache.james.mime4j.dom.address.AddressList;
import org.apache.james.mime4j.dom.address.Mailbox;
import org.apache.james.mime4j.field.AddressListFieldImpl;
import org.apache.james.mime4j.field.DefaultFieldParser;
import org.apache.james.mime4j.field.address.AddressBuilder;
import org.apache.james.mime4j.field.address.ParseException;
import org.apache.james.mime4j.stream.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class StructuredMailHeader {

    private static final Logger LOG = LoggerFactory.getLogger(StructuredMailHeader.class);

    private static final HeaderDecoder HEADER_DECODER = new HeaderDecoder();

    private ImmutableMultimap<String, String> decodedNormalizedHeaders;

    private final Message mail;

    StructuredMailHeader(Message mail) {
        this.mail = mail;
        revalidateHeaders();
    }

    public void removeHeader(String name) {
        mail.getHeader().removeFields(name);
        revalidateHeaders();
    }

    public void setFrom(String address) {
        try {
            mail.setFrom(asMailbox(address));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        revalidateHeaders();
    }

    public void setTo(String address) {
        try {
            mail.setTo(asMailbox(address));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        revalidateHeaders();
    }

    public void setReplyTo(String address) {
        try {
            mail.setReplyTo(asMailbox(address));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        revalidateHeaders();
    }

    public void addHeader(String name, String value) {
        try {
            String normalizedHeaderName = HEADER_DECODER.normalizeHeaderName(name);
            Field f = DefaultFieldParser.parse(String.format("%s: %s", normalizedHeaderName, value));
            mail.getHeader().setField(f);
            revalidateHeaders();
        } catch (MimeException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean containsHeader(String string) {
        return decodedNormalizedHeaders.containsKey(HEADER_DECODER.normalizeHeaderName(string));
    }

    public List<String> list(String name) {
        String decodedName = HEADER_DECODER.normalizeHeaderName(name);
        return containsHeader(decodedName) ?
                ImmutableList.copyOf(decodedNormalizedHeaders.get(decodedName))
                : Collections.<String>emptyList();
    }

    public Map<String, List<String>> all() {
        ImmutableMap.Builder<String, List<String>> builder = ImmutableMap.<String, List<String>>builder();
        for (String key : decodedNormalizedHeaders.keySet()) {
            builder.put(key, ImmutableList.copyOf(decodedNormalizedHeaders.get(key)));
        }
        return builder.build();
    }

    public Map<String, String> unique() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder();
        for (String key : decodedNormalizedHeaders.keySet()) {
            builder.put(key, decodedNormalizedHeaders.get(key).iterator().next());
        }
        return builder.build();
    }

    /**
     * rebuild header cache
     */
    private void revalidateHeaders() {
        decodedNormalizedHeaders = HEADER_DECODER.decodeHeaders(mail);
    }

    Optional<InternetAddress> getHeaderAsInternetAddress(String headername) {
        Field field = mail.getHeader().getField(headername);
        if (field == null) {
            return Optional.absent();
        }

        String fieldBody = field.getBody();
        try {
            return Optional.of(new InternetAddress(fieldBody));
        } catch (AddressException e) {
            return tryLenientAddressParsing(fieldBody, e);
        }
    }

    private Optional<InternetAddress> tryLenientAddressParsing(String fieldBody, AddressException originalException) {
        // Some iOS devices set From: <     <email@example.com> >
        int lastOpenBracket = fieldBody.lastIndexOf('<');
        int nextClosedBracket = fieldBody.indexOf('>', lastOpenBracket + 1);

        if (lastOpenBracket >= nextClosedBracket) {
            LOG.warn("Could not parse mail address '" + fieldBody + "'", originalException);
            return Optional.absent();
        }

        String stringBetweenBrackets = fieldBody.substring(lastOpenBracket + 1, nextClosedBracket);
        try {
            return Optional.of(new InternetAddress(stringBetweenBrackets));
        } catch (AddressException e) {
            LOG.warn("Could not parse mail address '" + fieldBody + "'", e);
            return Optional.absent();
        }
    }

    public List<String> getMailAddressesFromHeader(String fieldName) {
        Builder<String> resultBuilder = ImmutableList.<String>builder();
        AddressList addressList = AddressListFieldImpl.PARSER.parse(mail.getHeader().getField(fieldName), new DecodeMonitor()).getAddressList();
        for (Address address : addressList) {
            if (address instanceof Mailbox) {
                Mailbox mailbox = (Mailbox) address;
                resultBuilder.add(mailbox.getAddress());
            }
        }

        return resultBuilder.build();

    }

    /**
     * Properly escapes an address even when it contains a space but is not yet escaped.
     */
    private Mailbox asMailbox(String address) throws ParseException {
        return AddressBuilder.DEFAULT.parseMailbox(address);
    }

    public Map<String, String> customHeaders() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        String prefix = Mail.CUSTOM_HEADER_PREFIX.toLowerCase();
        for (Map.Entry<String, String> entry : decodedNormalizedHeaders.entries()) {

            String lowercaseKey = entry.getKey().toLowerCase();
            if (lowercaseKey.startsWith(prefix)) {
                String customVarName = lowercaseKey.substring(prefix.length());
                builder.put(customVarName, entry.getValue());
            }
        }
        return builder.build();
        // TODO: add unit test to see if this works!
    }


}
