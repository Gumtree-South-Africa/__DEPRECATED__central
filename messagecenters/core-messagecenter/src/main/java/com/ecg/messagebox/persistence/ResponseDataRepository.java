package com.ecg.messagebox.persistence;

import com.ecg.messagebox.model.ResponseData;

import java.util.List;

/**
 * Response data repository.
 */
public interface ResponseDataRepository {

    /**
     * Return the list with response data per conversation for the user.
     *
     * @param userId the user id
     * @return the list with response data
     */
    List<ResponseData> getResponseData(String userId);

    /**
     * Add or update the response data for the conversation of the user.
     *
     * @param responseData the response data
     */
    void addOrUpdateResponseDataAsync(ResponseData responseData);
}
