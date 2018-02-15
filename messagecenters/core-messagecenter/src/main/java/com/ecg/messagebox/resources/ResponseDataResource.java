package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.messagebox.resources.exceptions.NotFoundException;
import com.ecg.messagebox.resources.responses.ResponseDataResponse;
import com.ecg.messagebox.service.ResponseDataService;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class ResponseDataResource {

    private final Timer getResponseDataTimer = TimingReports.newTimer("webapi.get-response-data");
    private final Timer getAggregatedResponseDataTimer = TimingReports.newTimer("webapi.get-aggregated-response-data");

    private final ResponseDataService responseDataService;

    @Autowired
    public ResponseDataResource(ResponseDataService responseDataService) {
        this.responseDataService = responseDataService;
    }

    @GetMapping("/users/{userId}/response-data")
    public List<ResponseDataResponse> getResponseData(@PathVariable String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            return responseDataService.getResponseData(userId).stream()
                    .map(ResponseDataResponse::new)
                    .collect(Collectors.toList());
        }
    }

    @GetMapping("/users/{userId}/aggregated-response-data")
    public AggregatedResponseData getAggregatedResponseData(@PathVariable String userId) {
        try (Timer.Context ignored = getAggregatedResponseDataTimer.time()) {
            return responseDataService.getAggregatedResponseData(userId)
                    .orElseThrow(NotFoundException::new);
        }
    }
}
