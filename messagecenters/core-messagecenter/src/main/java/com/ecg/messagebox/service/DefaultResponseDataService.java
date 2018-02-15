package com.ecg.messagebox.service;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.persistence.ResponseDataRepository;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.joda.time.Minutes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;

@Component
public class DefaultResponseDataService implements ResponseDataService {

    private final ResponseDataRepository responseDataRepository;
    private final Timer getResponseDataTimer = newTimer("postBoxService.v2.getResponseData");

    private static final String X_MESSAGE_TYPE = "X-Message-Type";
    private final UserIdentifierService userIdentifierService;

    @Autowired
    public DefaultResponseDataService(ResponseDataRepository responseDataRepository, UserIdentifierService userIdentifierService) {
        this.responseDataRepository = responseDataRepository;
        this.userIdentifierService = userIdentifierService;
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            return responseDataRepository.getResponseData(userId);
        }
    }

    @Override
    public Optional<AggregatedResponseData> getAggregatedResponseData(String userId) {
        return ResponseDataCalculator.calculate(responseDataRepository.getResponseData(userId));
    }

    @Override
    public void calculateResponseData(String userId, Conversation rtsConversation, Message rtsNewMessage) {
        // BR: only for sellers
        boolean isSeller = userIdentifierService.getSellerUserId(rtsConversation).map(userId::equals).orElse(false);
        if (isSeller) {
            // BR: only for conversations initiated by buyer
            if (rtsConversation.getMessages().size() == 1 && MessageDirection.BUYER_TO_SELLER == rtsNewMessage.getMessageDirection()) {
                ResponseData initialResponseData = new ResponseData(userId, rtsConversation.getId(), rtsConversation.getCreatedAt(),
                        MessageType.getWithEmailAsDefault(rtsNewMessage.getHeaders().get(X_MESSAGE_TYPE)));
                responseDataRepository.addOrUpdateResponseDataAsync(initialResponseData);
            } else if (rtsConversation.getMessages().size() > 1 && MessageDirection.BUYER_TO_SELLER == rtsConversation.getMessages().get(0).getMessageDirection()) {
                // BR: only consider the first response from seller
                java.util.Optional<com.ecg.replyts.core.api.model.conversation.Message> firstSellerToBuyerMessage = rtsConversation.getMessages().stream()
                        .filter(message -> MessageDirection.SELLER_TO_BUYER == message.getMessageDirection()).findFirst();
                if (firstSellerToBuyerMessage.isPresent() && firstSellerToBuyerMessage.get().getId().equals(rtsNewMessage.getId())) {
                    int responseSpeed = Minutes.minutesBetween(rtsConversation.getCreatedAt(), rtsNewMessage.getReceivedAt()).getMinutes();
                    // Only the response speed value is different from the initially created response data. The conversation type is the type of the first message.
                    ResponseData updatedResponseData = new ResponseData(userId, rtsConversation.getId(), rtsConversation.getCreatedAt(),
                            MessageType.getWithEmailAsDefault(rtsConversation.getMessages().get(0).getHeaders().get(X_MESSAGE_TYPE)), responseSpeed);
                    responseDataRepository.addOrUpdateResponseDataAsync(updatedResponseData);
                }
            }
        }
    }
}
