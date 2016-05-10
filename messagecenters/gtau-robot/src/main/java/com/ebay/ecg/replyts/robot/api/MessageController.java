package com.ebay.ecg.replyts.robot.api;

import com.codahale.metrics.Timer;
import com.ebay.ecg.replyts.robot.api.requests.GetConversationsByAdIdAndEmailCommand;
import com.ebay.ecg.replyts.robot.api.requests.PostMessageToConversationCommand;
import com.ebay.ecg.replyts.robot.api.requests.PostMessageToConversationsForAdCommand;
import com.ebay.ecg.replyts.robot.api.requests.payload.MessagePayload;
import com.ebay.ecg.replyts.robot.api.requests.payload.ResponsePayload;
import com.ebay.ecg.replyts.robot.service.RobotService;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.apache.james.mime4j.MimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

@Controller
class MessageController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageController.class);

    private static final Timer API_ROBOT_POST_TO_CONVERSATION_BY_ID = TimingReports.newTimer("api-robot-post-to-conversation-by-id");
    private static final Timer API_ROBOT_POST_TO_CONVERSATIONS_BY_AD_ID = TimingReports.newTimer("api-robot-post-to-conversations-by-ad-id");
    private static final Timer API_ROBOT_CONVERSATIONS_BY_AD_ID = TimingReports.newTimer("api-robot-conversations-by-ad-id");


    private final RobotService robotService;

    @Autowired
    public MessageController(RobotService robotService) {
        this.robotService = robotService;
    }


    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = PostMessageToConversationCommand.MAPPING,
            consumes = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.POST})
    @ResponseBody
    ResponseObject<?> postMessageToConversation(
            @PathVariable String conversationId,
            @RequestBody MessagePayload payload) throws IOException, MimeException {

        Timer.Context timerContext = API_ROBOT_POST_TO_CONVERSATION_BY_ID.time();

        try {
            robotService.addMessageToConversation(conversationId, payload);

            return ResponseObject.of(RequestState.OK);

        } finally {
            timerContext.stop();
        }
    }

    @RequestMapping(value = PostMessageToConversationsForAdCommand.MAPPING,
            consumes = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.POST})
    @ResponseBody
    ResponseObject<?> postMessageToConversationsForAd(
            @PathVariable String email,
            @PathVariable String adId,
            @RequestBody MessagePayload payload) throws IOException, MimeException {

        Timer.Context timerContext = API_ROBOT_POST_TO_CONVERSATIONS_BY_AD_ID.time();

        try {
            return createResponse(robotService.addMessageToConversationsForAd(email, adId, payload));
        } finally {
            timerContext.stop();
        }
    }

    @RequestMapping(value = GetConversationsByAdIdAndEmailCommand.MAPPING,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @ResponseBody
    ResponseObject<?> getConversationsByAdIdAndEmail(
            @PathVariable String email,
            @PathVariable String adId) {

        Timer.Context timerContext = API_ROBOT_CONVERSATIONS_BY_AD_ID.time();

        try {
            return ResponseObject.of(robotService.getConversationsForAd(email, adId));
        } finally {
            timerContext.stop();
        }
    }

    private ResponseObject<ResponsePayload> createResponse(List<String> errors) {
        ResponsePayload response = new ResponsePayload();
        response.setErrors(errors);
        response.setStatus(RequestState.OK.name());
        return ResponseObject.of(response);
    }
}
