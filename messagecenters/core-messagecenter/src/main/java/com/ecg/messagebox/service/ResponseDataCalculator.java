package com.ecg.messagebox.service;

import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.model.AggregatedResponseData;
import org.joda.time.DateTime;
import static com.ecg.messagebox.model.MessageType.*;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class ResponseDataCalculator {

    private static final AggregatedResponseData DEFAULT_TOTAL_RESPONSE_DATA = new AggregatedResponseData(-1, 0);
    static final int MIN_NR_OF_CONVERSATIONS_FOR_RESPONSE_DATA = 10;
    static final int DAYS = 2;

    static Optional<AggregatedResponseData> calculate(final List<ResponseData> responseDataList) {
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
