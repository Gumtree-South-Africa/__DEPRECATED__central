package com.ecg.de.mobile.replyts.rating.svc;

import de.mobile.dealer.rating.invite.EmailInviteEntity;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.PUT;

/**
 * Created by vbalaramiah on 4/23/15.
 */
public interface DealerRatingServiceClient {

    @Headers({
            "Content-type:application/json",
            "Accept:application/json"
    })
    @PUT("/emailInvite")
    Response createEmailInvite(@Body EmailInviteEntity invite);
}
