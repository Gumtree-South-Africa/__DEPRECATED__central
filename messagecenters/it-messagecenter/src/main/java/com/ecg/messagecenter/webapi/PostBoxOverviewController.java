package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.persistence.ConversationThread;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.webapi.requests.MessageCenterDeletePostBoxConversationCommandNew;
import com.ecg.messagecenter.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.persistence.ConversationRepository;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

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

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

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

        PostBox<ConversationThread> postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        postBoxRepository.deleteConversations(postBox, Arrays.asList(ids));
        return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
    }


    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }
}
