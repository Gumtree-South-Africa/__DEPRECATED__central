package com.ecg.de.mobile.replyts.comafilterservice.filters;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

class FilterServiceErrorHandler implements ErrorHandler {
    @Override public Throwable handleError(RetrofitError cause) {
        Response r = cause.getResponse();
        if (r != null && r.getStatus() >= 400) {
            return new RuntimeException("Error caught during coma filter service request.",cause);
        }
        return cause;
    }
}
