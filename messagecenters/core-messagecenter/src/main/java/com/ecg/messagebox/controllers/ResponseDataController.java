package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.responses.ResponseDataResponse;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.service.ResponseDataService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
public class ResponseDataController {

    private final Timer getResponseDataTimer = TimingReports.newTimer("webapi.get-response-data");
    private final Timer getAggregatedResponseDataTimer = TimingReports.newTimer("webapi.get-aggregated-response-data");

    private final ResponseDataService responseDataService;

    @Autowired
    public ResponseDataController(ResponseDataService responseDataService) {
        this.responseDataService = responseDataService;
    }

    @GetMapping("/users/{userId}/response-data")
    public ResponseObject<ResponseDataResponse> getResponseData(@PathVariable String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            List<ResponseData> responseDataList = responseDataService.getResponseData(userId);
            return ResponseObject.of(new ResponseDataResponse(responseDataList));
        }
    }

    @GetMapping("/users/{userId}/aggregated-response-data")
    public ResponseObject<?> getAggregatedResponseData(@PathVariable String userId) {
        try (Timer.Context ignored = getAggregatedResponseDataTimer.time()) {
            Optional<AggregatedResponseData> responseData = responseDataService.getAggregatedResponseData(userId);
            return ResponseObject.of(responseData.isPresent() ? responseData.get() : RequestState.ENTITY_NOT_FOUND);
        }
    }
}
