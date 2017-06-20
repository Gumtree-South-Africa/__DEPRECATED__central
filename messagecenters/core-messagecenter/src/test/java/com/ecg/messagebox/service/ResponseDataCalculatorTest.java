package com.ecg.messagebox.service;

import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.model.AggregatedResponseData;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.Collections;
import java.util.Optional;

import static java.util.Optional.of;
import static java.util.Optional.empty;
import static com.ecg.messagebox.service.ResponseDataCalculator.*;
import static com.ecg.messagebox.model.MessageType.*;
import static java.util.Arrays.*;
import static org.junit.Assert.assertEquals;

public class ResponseDataCalculatorTest {

    private static final String CONVERSATION = "da3d4a4sds1";
    private static final String USER = "987655";
    private final AggregatedResponseData DEFAULT_RESPONSE_DATA = new AggregatedResponseData(-1, 0);

    private final DateTime oldEnoughDate = new DateTime().minusDays(DAYS + 10);

    @Test
    public void shouldNotTakeIntoAccountBidConversations() {
        ResponseData[] onlyBids = enoughResponseDatas();
        fill(onlyBids, new ResponseData(USER, CONVERSATION, oldEnoughDate, BID, 80));

        Optional<AggregatedResponseData> forBids = calculate(asList(onlyBids));
        assertEquals(empty(), forBids);
    }

    @Test
    public void shouldNotTakeIntoConversationsCreatedAndNotAnsweredWithinTwoDays() {
        ResponseData[] recent = enoughResponseDatas();
        fill(recent, new ResponseData(USER, CONVERSATION, new DateTime(), CHAT, -1));

        Optional<AggregatedResponseData> forRecent = calculate(asList(recent));
        assertEquals(empty(), forRecent);
    }

    @Test
    public void aggregatedResponseDataIsNotAvailableForNoOrSeveralRespondedConversations() {
        Optional<AggregatedResponseData> forEmpty = calculate(Collections.emptyList());
        assertEquals(empty(), forEmpty);

        ResponseData[] several = new ResponseData[MIN_NR_OF_CONVERSATIONS_FOR_RESPONSE_DATA - 1];
        fill(several, new ResponseData(USER, CONVERSATION, oldEnoughDate, CHAT, 80));
        
        Optional<AggregatedResponseData> forSeveral = calculate(asList(several));
        assertEquals(Optional.empty(), forSeveral);
    }

    @Test
    public void conversationsWithNegativeResponseSpeedShouldNotBeTakenInCalculation() {
        ResponseData[] notResponded = enoughResponseDatas();
        fill(notResponded, new ResponseData(USER, CONVERSATION, oldEnoughDate, CHAT, -1));
        Optional<AggregatedResponseData> forNotResponded = calculate(asList(notResponded));

        assertEquals(of(DEFAULT_RESPONSE_DATA), forNotResponded);
    }

    @Test
    public void aggregatedResponseDataIsAvailableOnlyAfterMinNrOfConversations() {
        ResponseData[] responseSpeedIsZero = enoughResponseDatas();
        fill(responseSpeedIsZero, new ResponseData(USER, CONVERSATION, oldEnoughDate, CHAT, 10));
        responseSpeedIsZero[0] = new ResponseData(USER, CONVERSATION, oldEnoughDate, EMAIL, 55);
        responseSpeedIsZero[1] = new ResponseData(USER, CONVERSATION, oldEnoughDate, EMAIL, 128);
        responseSpeedIsZero[2] = new ResponseData(USER, CONVERSATION, oldEnoughDate, EMAIL, 47);
        responseSpeedIsZero[3] = new ResponseData(USER, CONVERSATION, oldEnoughDate, EMAIL, 147);
        responseSpeedIsZero[4] = new ResponseData(USER, CONVERSATION, oldEnoughDate, EMAIL, 68);
        responseSpeedIsZero[5] = new ResponseData(USER, CONVERSATION, oldEnoughDate, EMAIL, -1);
        responseSpeedIsZero[6] = new ResponseData(USER, CONVERSATION, DateTime.now(), EMAIL, -1);
        responseSpeedIsZero[7] = new ResponseData(USER, CONVERSATION, oldEnoughDate, BID, 68);

        Optional<AggregatedResponseData> allResponded = calculate(asList(responseSpeedIsZero));
        assertEquals(of(new AggregatedResponseData(47, 94)), allResponded);

    }

    private static ResponseData[] enoughResponseDatas() {
        return new ResponseData[MIN_NR_OF_CONVERSATIONS_FOR_RESPONSE_DATA + 10];
    }
}
