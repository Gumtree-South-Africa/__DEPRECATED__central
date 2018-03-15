package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.responses.ResponseDataResponse;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.persistence.ResponseDataRepository;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResponseDataControllerTest {

    private static final String USER = "someUser";
    private static final String CONVERSATION = "someConv";

    @Mock
    private ResponseDataRepository responseDataRepository;
    private ResponseDataController responseDataController;

    @Before
    public void setUp() throws Exception {
        responseDataController = new ResponseDataController(responseDataRepository);
    }

    @Test
    public void noAggregateResponseData_entityNotFoundReturned() {
        when(responseDataRepository.getResponseData(USER)).thenReturn(Collections.emptyList());

        ResponseObject<?> response = responseDataController.getAggregatedResponseData(USER);
        assertThat(response.getBody(), equalTo(RequestState.ENTITY_NOT_FOUND));

        verify(responseDataRepository).getResponseData(USER);
    }

    @Test
    public void returnsAggregateResponseDataWhenPresent() {
        ResponseData responseData1 = new ResponseData(USER, "conversation-1", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData2 = new ResponseData(USER, "conversation-2", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData3 = new ResponseData(USER, "conversation-3", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData4 = new ResponseData(USER, "conversation-4", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData5 = new ResponseData(USER, "conversation-5", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData6 = new ResponseData(USER, "conversation-6", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData7 = new ResponseData(USER, "conversation-7", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData8 = new ResponseData(USER, "conversation-8", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData9 = new ResponseData(USER, "conversation-9", new DateTime(0), MessageType.CHAT, -1);
        ResponseData responseData10 = new ResponseData(USER, "conversation-10", new DateTime(0), MessageType.CHAT, -1);
        List<ResponseData> responseData = Arrays.asList(responseData1, responseData2, responseData3, responseData4, responseData5,
                responseData6, responseData7, responseData8, responseData9, responseData10);

        AggregatedResponseData aggregatedResponseData = new AggregatedResponseData(-1, 0);
        when(responseDataRepository.getResponseData(USER)).thenReturn(responseData);

        ResponseObject<?> response = responseDataController.getAggregatedResponseData(USER);
        assertThat(response.getBody(), equalTo(aggregatedResponseData));

        verify(responseDataRepository).getResponseData(USER);
    }

    @Test
    public void returnsResponseDataForEachConversation() {
        DateTime now = DateTime.now();
        ResponseData responseData = new ResponseData(USER, CONVERSATION, now, MessageType.CHAT, 90);
        List<ResponseData> responseDataList = Collections.singletonList(responseData);
        when(responseDataRepository.getResponseData(USER)).thenReturn(responseDataList);

        ResponseObject<ResponseDataResponse> response = responseDataController.getResponseData(USER);
        assertThat(response.getBody().getResponseData().size(), equalTo(1));
        assertThat(response.getBody().getResponseData().get(0), equalTo(new ResponseDataResponse.ResponseData(responseData)));

        verify(responseDataRepository).getResponseData(USER);
    }

}
