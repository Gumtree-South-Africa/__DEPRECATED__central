package com.ecg.de.mobile.replyts.rating.svc;

import javax.servlet.http.HttpServletResponse;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by vbalaramiah on 4/23/15.
 */
public class DealerRatingServiceErrorHandler implements ErrorHandler {

    @Override public Throwable handleError(RetrofitError cause) {
        final Response response = cause.getResponse();
        if (response != null) {
            if (response.getStatus() == HttpServletResponse.SC_CONFLICT) {
                return new SkippingEmailInviteException();
            } else if (response.getStatus() >= HttpServletResponse.SC_BAD_REQUEST) {
                return new RuntimeException("Error caught during dealer rating service request.",cause);
            }
        }
        return cause;
    }
}
