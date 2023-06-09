package com.ecg.mde.filter.comaservice.filters;

import com.ecg.mde.filter.comaservice.FilterService;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;

import java.util.Date;
import java.util.Objects;

import static java.util.Optional.ofNullable;

public class ContactMessageAssembler {

    public ContactMessage getContactMessage(MessageProcessingContext context) {

        ContactMessage message = new ContactMessage();

        message.setConversationId(context.getConversation().getId());

        ofNullable(context.getMessage().getCaseInsensitiveHeaders().get(FilterService.CUSTOM_HEADER_FROM_USERID))
                .ifPresent(message::setFromUserId);

        message.setBuyerMailAddress(context.getConversation()
                .getUserIdFor(context.getMessageDirection().getFromRole()));

        Date messageCreatedTime = context.getMail().map(Mail::getSentDate).filter(Objects::nonNull).orElse(new Date());
        message.setMessageCreatedTime(messageCreatedTime);

        if (context.getMail().get().getPlaintextParts().size() > 0) {
            message.setMessage(context.getMail().get().getPlaintextParts().get(0));
        }

        String sellerType = getSellerType(context);
        if (sellerType != null) {
            message.setSellerType(sellerType);
        }

        String siteId = getSiteId(context);
        if (siteId != null) {
            message.setSiteId(siteId);
        }

        try {
            PhoneNumber phoneNumber = getPhoneNumber(context);
            if (phoneNumber != null) {
                message.setBuyerPhoneNumber(phoneNumber);
            }
        } catch (IllegalArgumentException e) {
            // skipped
        }

        String ipAddressV4V6 = getIpAddressV4V6(context);
        if (ipAddressV4V6 != null) {
            message.setIpAddressV4V6(ipAddressV4V6);
        }

        return message;
    }

    private String getSiteId(MessageProcessingContext context) {
        return context.getMessage().getCaseInsensitiveHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Seller_Site_Id");
    }

    private String getSellerType(MessageProcessingContext context) {
        return context.getMessage().getCaseInsensitiveHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Seller_Type");
    }

    private PhoneNumber getPhoneNumber(MessageProcessingContext context) {
        String countryCode = context.getMessage().getCaseInsensitiveHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Phone_Number_Country_Code");
        String displayNumber = context.getMessage().getCaseInsensitiveHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Phone_Number_Display_Number");
        if (countryCode == null || displayNumber == null) {
            return null;
        }
        return new PhoneNumber(Integer.valueOf(countryCode), displayNumber);

    }

    private String getIpAddressV4V6(MessageProcessingContext context) {
        return context.getMessage().getCaseInsensitiveHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Ip_Address_V4V6");
    }
}
