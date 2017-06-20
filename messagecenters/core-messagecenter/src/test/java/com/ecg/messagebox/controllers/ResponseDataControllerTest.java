package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.responses.ResponseDataResponse;
import com.ecg.messagebox.service.ResponseDataService;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResponseDataControllerTest {

    private static final String USER = "someUser";
    private static final String CONVERSATION = "someConv";

    @Mock
    private ResponseDataService responseDataService;
    private ResponseDataController responseDataController;

    @Before
    public void setUp() throws Exception {
     responseDataController = new ResponseDataController(responseDataService);
    }

    @Test
    public void noAggregateResponseData_entityNotFoundReturned() throws Exception {
        when(responseDataService.getAggregatedResponseData(USER)).thenReturn(Optional.empty());

        ResponseObject<?> response = responseDataController.getAggregatedResponseData(USER);
        assertThat(response.getBody(), equalTo(RequestState.ENTITY_NOT_FOUND));

        verify(responseDataService).getAggregatedResponseData(USER);
    }

    @Test
    public void returnsAggregateResponseDataWhenPresent() throws Exception {
        AggregatedResponseData aggregatedResponseData = new AggregatedResponseData(-1, 0);
        when(responseDataService.getAggregatedResponseData(USER)).thenReturn(Optional.of(aggregatedResponseData));

        ResponseObject<?> response = responseDataController.getAggregatedResponseData(USER);
        assertThat(response.getBody(), equalTo(aggregatedResponseData));

        verify(responseDataService).getAggregatedResponseData(USER);
    }

    @Test
    public void returnsResponseDataForEachConversation() throws Exception {
        DateTime now = DateTime.now();
        ResponseData responseData = new ResponseData(USER, CONVERSATION, now, MessageType.CHAT, 90);
        List<ResponseData> responseDataList = Collections.singletonList(responseData);
        when(responseDataService.getResponseData(USER)).thenReturn(responseDataList);

        ResponseObject<ResponseDataResponse> response = responseDataController.getResponseData(USER);
        assertThat(response.getBody().getResponseData().size(), equalTo(1));
        assertThat(response.getBody().getResponseData().get(0), equalTo(new ResponseDataResponse.ResponseData(responseData)));

        verify(responseDataService).getResponseData(USER);
    }

}
