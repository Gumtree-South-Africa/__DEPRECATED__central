package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.responses.ResponseDataResponse;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.messagebox.model.ResponseData;
import com.ecg.messagebox.persistence.ResponseDataRepository;
import com.ecg.messagebox.service.ResponseDataCalculator;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ResponseDataController {

    private final Timer getResponseDataTimer = TimingReports.newTimer("webapi.get-response-data");
    private final Timer getAggregatedResponseDataTimer = TimingReports.newTimer("webapi.get-aggregated-response-data");

    private final ResponseDataRepository responseDataRepository;

    @Autowired
    public ResponseDataController(ResponseDataRepository responseDataRepository) {
        this.responseDataRepository = responseDataRepository;
    }

    @GetMapping("/users/{userId}/response-data")
    public ResponseObject<ResponseDataResponse> getResponseData(@PathVariable String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            List<ResponseData> responseDataList = responseDataRepository.getResponseData(userId);
            return ResponseObject.of(new ResponseDataResponse(responseDataList));
        }
    }

    @GetMapping("/users/{userId}/aggregated-response-data")
    public ResponseObject<?> getAggregatedResponseData(@PathVariable String userId) {
        try (Timer.Context ignored = getAggregatedResponseDataTimer.time()) {
            Optional<AggregatedResponseData> responseData = ResponseDataCalculator.calculate(responseDataRepository.getResponseData(userId));
            return ResponseObject.of(responseData.isPresent() ? responseData.get() : RequestState.ENTITY_NOT_FOUND);
        }
    }
}
