package com.ecg.messagecenter.webapi;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.ecg.messagebox.controllers.TopLevelExceptionHandler;
import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.requests.MessageCenterDeletePostBoxConversationCommandNew;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

/**
 * User: maldana
 * Date: 24.10.13
 * Time: 14:06
 *
 * @author maldana@ebay.de
 */
@Controller class PostBoxOverviewController {
    private static final Logger LOG = LoggerFactory.getLogger(PostBoxOverviewController.class);

    private static final Timer API_POSTBOX_BY_EMAIL = TimingReports.newTimer("webapi-postbox-by-email");
    private static final Timer API_POSTBOX_CONVERSATION_DELETE_BY_ID = TimingReports.newTimer("webapi-postbox-conversation-delete");

    private static final Histogram API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX = TimingReports.newHistogram("webapi-postbox-num-conversations-of-postbox");

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;

    @Autowired public PostBoxOverviewController(ConversationRepository conversationRepository,
                                                SimplePostBoxRepository postBoxRepository) {
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationRepository);
    }

    @InitBinder public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response) throws IOException {
        TopLevelExceptionHandler.handle(ex, response);
    }

    @RequestMapping(value = MessageCenterGetPostBoxCommand.MAPPING,
                    produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET,
                    RequestMethod.PUT}) @ResponseBody
    ResponseObject<PostBoxResponse> getPostBoxByEmail(@PathVariable String email,
                    @RequestParam(value = "newCounterMode", defaultValue = "false")
                    boolean newCounterMode,
                    @RequestParam(value = "size", defaultValue = "50", required = false)
                    Integer size,
                    @RequestParam(value = "page", defaultValue = "0", required = false)
                    Integer page,
                    @RequestParam(value = "robotEnabled", defaultValue = "true", required = false)
                    boolean robotEnabled, HttpServletRequest request) {

        Timer.Context timerContext = API_POSTBOX_BY_EMAIL.time();

        try {
            PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

            API_NUM_REQUESTED_NUM_CONVERSATIONS_OF_POSTBOX
                            .update(postBox.getConversationThreads().size());

            if (markAsRead(request)) {
                postBox.resetReplies();
                postBoxRepository.markConversationsAsRead(postBox, postBox.getConversationThreads());
            }

            if (robotEnabled) {
                return responseBuilder
                                .buildPostBoxResponse(email, size, page, postBox, newCounterMode);
            } else {
                return responseBuilder.buildPostBoxResponseRobotExcluded(email, size, page, postBox,
                                newCounterMode);
            }
        } catch (RuntimeException e) {
            LOG.error("Runtime Exception in PostBoxOverviewController: ", e);
            throw e;

        } finally {
            timerContext.stop();
        }
    }



    @RequestMapping(value = MessageCenterDeletePostBoxConversationCommandNew.MAPPING,
                    produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE)
    @ResponseBody
    ResponseObject<PostBoxResponse> removePostBoxConversationByEmailAndBulkConversationIds(
                    @PathVariable("email") String email,
                    @RequestParam(value = "ids", defaultValue = "") String[] ids,
                    @RequestParam(value = "newCounterMode", defaultValue = "true")
                    boolean newCounterMode,
                    @RequestParam(value = "page", defaultValue = "0", required = false)
                    Integer page,
                    @RequestParam(value = "size", defaultValue = "50", required = false)
                    Integer size) {

        Timer.Context timerContext = API_POSTBOX_CONVERSATION_DELETE_BY_ID.time();

        try {
            PostBox<ConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
            postBoxRepository.deleteConversations(postBox, Arrays.asList(ids));
            return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);

        } finally {
            timerContext.stop();
        }
    }


    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }



}
