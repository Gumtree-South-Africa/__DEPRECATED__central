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
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@Controller
public class ResponseDataController {

    private static final String MAPPING = "/users/{userId}/response-data";

    private static final String AGGREGATED_MAPPING = "/users/{userId}/aggregated-response-data";

    private final Timer getResponseDataTimer = TimingReports.newTimer("webapi.get-response-data");
    private final Timer getAggregatedResponseDataTimer = TimingReports.newTimer("webapi.get-aggregated-response-data");

    private final ResponseDataService responseDataService;

    @Autowired
    public ResponseDataController(ResponseDataService responseDataService) {
        this.responseDataService = responseDataService;
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response) throws IOException {
        TopLevelExceptionHandler.handle(ex, response);
    }

    @RequestMapping(value = MAPPING, method = GET)
    @ResponseBody
    public ResponseObject<ResponseDataResponse> getResponseData(@PathVariable String userId) {

        try (Timer.Context ignored = getResponseDataTimer.time()) {
            List<ResponseData> responseDataList = responseDataService.getResponseData(userId);
            return ResponseObject.of(new ResponseDataResponse(responseDataList));
        }
    }

    @RequestMapping(value = AGGREGATED_MAPPING, method = GET)
    @ResponseBody
    public ResponseObject<?> getAggregatedResponseData(@PathVariable String userId) {
        try (Timer.Context ignored = getAggregatedResponseDataTimer.time()) {
            Optional<AggregatedResponseData> responseData = responseDataService.getAggregatedResponseData(userId);
            return ResponseObject.of(responseData.isPresent() ? responseData.get() : RequestState.ENTITY_NOT_FOUND);
        }
    }
}
