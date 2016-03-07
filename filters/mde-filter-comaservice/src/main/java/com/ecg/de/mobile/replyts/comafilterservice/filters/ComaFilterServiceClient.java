package com.ecg.de.mobile.replyts.comafilterservice.filters;

import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;

import java.util.List;

public interface ComaFilterServiceClient {


    @Headers("Content-Type: application/json")
    @POST("/filter")
    List<String> getFilterResults(@Body ContactMessage message);

}
