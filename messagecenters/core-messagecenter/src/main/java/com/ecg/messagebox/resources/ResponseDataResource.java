package com.ecg.messagebox.resources;

import com.ecg.messagebox.persistence.ResponseDataRepository;
import com.ecg.messagebox.resources.exceptions.ClientException;
import com.ecg.messagebox.resources.responses.AggregatedResponseDataResponse;
import com.ecg.messagebox.resources.responses.ErrorResponse;
import com.ecg.messagebox.resources.responses.ResponseDataResponse;
import com.ecg.messagebox.service.ResponseDataCalculator;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    private final ResponseDataRepository responseDataRepository;

    @Autowired
    public ResponseDataResource(ResponseDataRepository responseDataRepository) {
        this.responseDataRepository = responseDataRepository;
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
        return responseDataRepository.getResponseData(userId).stream()
                .map(ResponseDataResponse::new)
                .collect(Collectors.toList());
    }

    @ApiOperation(
            value = "Get aggregated response statistics",
            notes = "Retrieve a single object containing aggregated statistics about user's responses",
            nickname = "getAggregatedResponseData",
            tags = "Conversations")
    @ApiResponses({
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 404, message = "Aggregation Response Data Not Found", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Internal Error", response = ErrorResponse.class)
    })
    @GetMapping("/users/{userId}/aggregated-response-data")
    public AggregatedResponseDataResponse getAggregatedResponseData(
            @ApiParam(value = "User ID", required = true) @PathVariable String userId) {
        return ResponseDataCalculator.calculate(responseDataRepository.getResponseData(userId))
                .map(data -> new AggregatedResponseDataResponse(data.getSpeed(), data.getRate()))
                .orElseThrow(() -> new ClientException(HttpStatus.NOT_FOUND, String.format("AggregationResponseData not found for userID: %s", userId)));
    }
}
