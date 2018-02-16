package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.model.AggregatedResponseData;
import com.ecg.messagebox.resources.exceptions.NotFoundException;
import com.ecg.messagebox.resources.responses.ResponseDataResponse;
import com.ecg.messagebox.service.ResponseDataService;
import com.ecg.replyts.core.runtime.TimingReports;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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

    @ApiOperation(value = "Get statistics about user responses", notes = "Retrieve a collection of response statistics belonging to specified user")
    @ApiResponses(@ApiResponse(code = 200, message = "Success"))
    @GetMapping("/users/{userId}/response-data")
    public List<ResponseDataResponse> getResponseData(
            @ApiParam(value = "User ID", required = true) @PathVariable String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            return responseDataService.getResponseData(userId).stream()
                    .map(ResponseDataResponse::new)
                    .collect(Collectors.toList());
        }
    }

    @ApiOperation(value = "Get aggregated response statistics", notes = "Retrieve a single object containing aggregated statistics about user's responses")
    @ApiResponses(@ApiResponse(code = 200, message = "Success"))
    @GetMapping("/users/{userId}/aggregated-response-data")
    public AggregatedResponseData getAggregatedResponseData(
            @ApiParam(value = "User ID", required = true) @PathVariable String userId) {
        try (Timer.Context ignored = getAggregatedResponseDataTimer.time()) {
            return responseDataService.getAggregatedResponseData(userId)
                    .orElseThrow(NotFoundException::new);
        }
    }
}
