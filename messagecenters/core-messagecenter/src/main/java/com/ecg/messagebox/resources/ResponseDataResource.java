package com.ecg.messagebox.resources;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.resources.exceptions.NotFoundException;
import com.ecg.messagebox.resources.responses.AggregatedResponseDataResponse;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.resources.responses.ResponseDataResponse;
import com.ecg.messagebox.service.ResponseDataService;
import com.ecg.replyts.core.runtime.TimingReports;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Api(tags = "Conversations")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ResponseDataResource {

    private final Timer getResponseDataTimer = TimingReports.newTimer("webapi.get-response-data");
    private final Timer getAggregatedResponseDataTimer = TimingReports.newTimer("webapi.get-aggregated-response-data");

    private final ResponseDataService responseDataService;

    @Autowired
    public ResponseDataResource(ResponseDataService responseDataService) {
        this.responseDataService = responseDataService;
    }

    @ApiOperation(
            value = "Get statistics about user responses",
            notes = "Retrieve a collection of response statistics belonging to specified user",
            nickname = "getResponseData",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @GetMapping("/users/{userId}/response-data")
    public List<ResponseDataResponse> getResponseData(
            @ApiParam(value = "User ID", required = true) @PathVariable String userId) {
        try (Timer.Context ignored = getResponseDataTimer.time()) {
            return responseDataService.getResponseData(userId).stream()
                    .map(ResponseDataResponse::new)
                    .collect(Collectors.toList());
        }
    }

    @ApiOperation(
            value = "Get aggregated response statistics",
            notes = "Retrieve a single object containing aggregated statistics about user's responses",
            nickname = "getAggregatedResponseData",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @GetMapping("/users/{userId}/aggregated-response-data")
    public AggregatedResponseDataResponse getAggregatedResponseData(
            @ApiParam(value = "User ID", required = true) @PathVariable String userId) {
        try (Timer.Context ignored = getAggregatedResponseDataTimer.time()) {
            return responseDataService.getAggregatedResponseData(userId)
                    .map(data -> new AggregatedResponseDataResponse(data.getSpeed(), data.getRate()))
                    .orElseThrow(() -> new NotFoundException("EntityNotFound", String.format("AggregationResponseData not found for userID: %s", userId)));
        }
    }
}
