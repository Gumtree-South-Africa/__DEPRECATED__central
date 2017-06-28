package com.ecg.de.mobile.replyts.comafilterservice.filters;

import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;

import java.util.List;

public interface ComaFilterServiceClient {


    @Headers("Content-Type: application/json")
    @POST("/coma-filter/message")
    List<String> getFilterResultsForMessage(@Body ContactMessage message);

    @Headers("Content-Type: application/json")
    @POST("/coma-filter/conversation")
    List<String> getFilterResultsForConversation(@Body ContactMessage message);

}
