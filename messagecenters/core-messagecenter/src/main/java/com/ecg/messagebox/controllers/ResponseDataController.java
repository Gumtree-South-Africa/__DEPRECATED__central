package com.ecg.messagebox.controllers;

import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.responses.ResponseDataResponse;
import com.ecg.messagebox.service.ResponseDataService;
import com.ecg.messagecenter.persistence.ResponseData;
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
import java.io.Writer;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
public class ResponseDataController {

    private static final String MAPPING = "/users/{userId}/response-data";

    private final Timer getResponseDataTimer = TimingReports.newTimer("webapi.get-response-data");

    private final ResponseDataService responseDataService;

    @Autowired
    public ResponseDataController(ResponseDataService responseDataService) {
        this.responseDataService = responseDataService;
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MAPPING, method = GET)
    @ResponseBody
    public ResponseObject<ResponseDataResponse> getResponseData(@PathVariable String userId) {

        try (Timer.Context ignored = getResponseDataTimer.time()) {
            List<ResponseData> responseDataList = responseDataService.getResponseData(userId);
            return ResponseObject.of(new ResponseDataResponse(responseDataList));
        }
    }
}
