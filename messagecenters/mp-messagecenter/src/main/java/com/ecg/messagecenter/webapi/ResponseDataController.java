package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.persistence.ResponseData;
import com.ecg.messagecenter.webapi.responses.ResponseDataResponse;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Controller
public class ResponseDataController {

    private static final Timer API_POSTBOX_GET_RESPONSE_DATA = TimingReports.newTimer("webapi-postbox-get-response-data");

    private static final String MAPPING = "/postboxes/{userId}/response-data";

    private final PostBoxService postBoxServiceDelegator;

    @Autowired
    public ResponseDataController(@Qualifier("postBoxServiceDelegator") PostBoxService postBoxServiceDelegator) {
        this.postBoxServiceDelegator = postBoxServiceDelegator;
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MAPPING, method = RequestMethod.GET)
    @ResponseBody
    public ResponseObject<ResponseDataResponse> getResponseData(@PathVariable String userId) {

        try (Timer.Context ignored = API_POSTBOX_GET_RESPONSE_DATA.time()) {
            List<ResponseData> responseDataList = postBoxServiceDelegator.getResponseData(userId);
            return ResponseObject.of(new ResponseDataResponse(responseDataList));
        }
    }
}