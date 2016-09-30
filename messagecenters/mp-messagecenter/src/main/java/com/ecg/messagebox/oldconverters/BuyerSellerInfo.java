package com.ecg.messagebox.oldconverters;

import com.ecg.messagebox.model.Participant;

import java.util.List;

import static com.ecg.messagebox.model.ParticipantRole.BUYER;
import static com.ecg.messagebox.model.ParticipantRole.SELLER;
import static java.lang.Long.parseLong;
import static java.util.Optional.ofNullable;

class BuyerSellerInfo {

    private Long buyerId;
    private Long sellerId;
    private String buyerName;
    private String sellerName;
    private String buyerEmail;
    private String sellerEmail;

    private BuyerSellerInfo(Long buyerId, Long sellerId,
                            String buyerName, String sellerName,
                            String buyerEmail, String sellerEmail) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.buyerEmail = buyerEmail;
        this.sellerEmail = sellerEmail;
    }

    Long getBuyerId() {
        return buyerId;
    }

    Long getSellerId() {
        return sellerId;
    }

    String getBuyerName() {
        return buyerName;
    }

    String getSellerName() {
        return sellerName;
    }

    String getBuyerEmail() {
        return buyerEmail;
    }

    String getSellerEmail() {
        return sellerEmail;
    }

    static class BuyerSellerInfoBuilder {

        final List<Participant> conversationParticipants;

        BuyerSellerInfoBuilder(List<Participant> conversationParticipants) {
            this.conversationParticipants = conversationParticipants;
        }

        BuyerSellerInfo build() {
            Participant participant1 = conversationParticipants.get(0);
            Participant participant2 = conversationParticipants.get(1);

            switch (participant1.getRole()) {
                case BUYER:
                    return new BuyerSellerInfo(
                            parseLong(participant1.getUserId()),
                            parseLong(participant2.getUserId()),
                            ofNullable(participant1.getName()).orElse(""),
                            ofNullable(participant2.getName()).orElse(""),
                            participant1.getEmail(),
                            participant2.getEmail()
                    );
                case SELLER:
                    return new BuyerSellerInfo(
                            parseLong(participant2.getUserId()),
                            parseLong(participant1.getUserId()),
                            ofNullable(participant2.getName()).orElse(""),
                            ofNullable(participant1.getName()).orElse(""),
                            participant2.getEmail(),
                            participant1.getEmail()
                    );
                default:
                    throw new IllegalArgumentException(
                            String.format("The only supported participant roles are %s and %s", BUYER.name(), SELLER.name()));
            }
        }
    }
}