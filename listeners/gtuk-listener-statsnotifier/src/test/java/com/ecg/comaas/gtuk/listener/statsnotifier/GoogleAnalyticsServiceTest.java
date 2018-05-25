package com.ecg.comaas.gtuk.listener.statsnotifier;

import com.codahale.metrics.Timer;
import com.ecg.comaas.gtuk.listener.statsnotifier.event.GAEvent;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Request;
import com.ning.http.client.RequestBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(MockitoJUnitRunner.class)
public class GoogleAnalyticsServiceTest {

    private GoogleAnalyticsService googleAnalyticsService;

    @Mock
    private AsyncHttpClient asyncHttpClient;

    @Before
    public void setUp() throws Exception {
        googleAnalyticsService = new GoogleAnalyticsService(asyncHttpClient, "UA-123456-00", "http://sample.com/pingme");
    }


    @Test
    public void itShouldPostTheRightPayLoadToGAEndpoint() throws Exception {
        //Given
        GAEvent testGAEvent = TestGA.create()
                .withEventCategory("TestEvent")
                .withClientId("aaaaa-bbbbb-ccccc-ddddd")
                .withCustomDimension(1, "customDimension")
                .build();
        //When
        googleAnalyticsService.sendAsyncEvent(testGAEvent, Optional.<Timer>empty());
        //Then
        ArgumentCaptor<Request> requestArgument = ArgumentCaptor.forClass(Request.class);
        ArgumentCaptor<AsyncHandler> handlerArgument = ArgumentCaptor.forClass(AsyncHandler.class);
        verify(asyncHttpClient).executeRequest(requestArgument.capture(), handlerArgument.capture());

        assertEquals(aRequest().getQueryParams(), requestArgument.getValue().getQueryParams());
        assertEquals(aRequest().getHeaders(), requestArgument.getValue().getHeaders());
    }

    @Test
    public void itShouldNotSendTheRequestIfMissingEventCategory() throws Exception {
        //Given
        GAEvent testGAEvent = TestGA.create()
                .withClientId("aaaaa-bbbbb-ccccc-ddddd")
                .build();
        //When
        googleAnalyticsService.sendAsyncEvent(testGAEvent, Optional.<Timer>empty());
        //Then
        verifyZeroInteractions(asyncHttpClient);
    }

    private Request aRequest() {
        return new RequestBuilder()
                .addHeader("Content-length", "0")
                .setMethod("POST")
                .setUrl("http://sample.com/pingme?" +
                        "v=1&t=event&tid=UA-123456-00&cd1=customDimension&ea=TestGA&ec=TestEvent&cid=aaaaa-bbbbb-ccccc-ddddd")
                .build();
    }
}