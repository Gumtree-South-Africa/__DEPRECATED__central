package com.ecg.messagebox.service;

import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.persistence.ResponseDataRepository;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.runtime.identifier.UserIdentifierService;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.ecg.messagebox.model.MessageType.BID;

@Component
public class ResponseDataCalculator {

    private static final AggregatedResponseData DEFAULT_TOTAL_RESPONSE_DATA = new AggregatedResponseData(-1, 0);
    static final int MIN_NR_OF_CONVERSATIONS_FOR_RESPONSE_DATA = 10;
    static final int DAYS = 2;

    private static final String X_MESSAGE_TYPE = "X-Message-Type";

    private final ResponseDataRepository responseDataRepository;
    private final UserIdentifierService userIdentifierService;

    @Autowired
    public ResponseDataCalculator(ResponseDataRepository responseDataRepository, UserIdentifierService userIdentifierService) {
        this.responseDataRepository = responseDataRepository;
        this.userIdentifierService = userIdentifierService;
    }

    /**
     * TODO: PB: Test this feature. Does this computation belong to MessageBox or Core? If MB remove CORE dependencies.
     */
    public void storeResponseData(String userId, Conversation rtsConversation, Message rtsNewMessage) {
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

    public static Optional<AggregatedResponseData> calculate(final List<ResponseData> responseDataList) {
        final List<ResponseData> validForCalculationDatas = filterOnlyResponseDatasForCalculation(responseDataList);
        if (validForCalculationDatas.size() < MIN_NR_OF_CONVERSATIONS_FOR_RESPONSE_DATA) {
            return Optional.empty();
        }
        final List<ResponseData> respondedConversations = filterRespondedConversations(validForCalculationDatas);
        if (respondedConversations.size() == 0) {
            return Optional.of(DEFAULT_TOTAL_RESPONSE_DATA);
        }

        final int responseRatePercentage = calculateResponseRatePercentage(validForCalculationDatas.size(), respondedConversations.size());
        final int eightyPercentileResponseSpeed = getEightyPercentileResponseSpeed(respondedConversations);
        return Optional.of(new AggregatedResponseData(eightyPercentileResponseSpeed, responseRatePercentage));
    }

    private static int calculateResponseRatePercentage(int totalNrOfConversations, int nrRespondedConversations) {
        // response rate is a number of responded conversations / total nr of conversations
        return (int) (((float) nrRespondedConversations / (float) totalNrOfConversations) * 100);
    }

    private static int getEightyPercentileResponseSpeed(List<ResponseData> respondedConversations) {
        int[] sortedResponseSpeedList = respondedConversations.stream().mapToInt(ResponseData::getResponseSpeed).sorted().toArray();
        // response speed is 80%tile from the sorted response speed list
        return sortedResponseSpeedList[(int)((sortedResponseSpeedList.length * 80) / 100F - 1)];
    }

    private static List<ResponseData> filterRespondedConversations(List<ResponseData> validForCalculationDatas) {
        return validForCalculationDatas.stream().filter(rd -> rd.getResponseSpeed() >= 0).collect(Collectors.toList());
    }

    private static List<ResponseData> filterOnlyResponseDatasForCalculation(List<ResponseData> responseDataList) {
        return responseDataList.stream().filter(notABidAndNotAnsweredInTheLastTwoDays()).collect(Collectors.toList());
    }

    private static Predicate<ResponseData> notABidAndNotAnsweredInTheLastTwoDays() {
        final long twoDaysAgo = new DateTime().minusDays(DAYS).getMillis();
        // Do not take into account bid conversations and conversations that were created and not answered in the last 2 days
        return rd -> rd.getConversationType() != BID && !(rd.getResponseSpeed() < 0  && rd.getConversationCreationDate().isAfter(twoDaysAgo));
    }
}
