package com.ecg.messagecenter.bt.webapi;

import com.ecg.messagecenter.bt.webapi.responses.PostBoxResponse;
import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.PostBoxId;
import com.ecg.messagecenter.core.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.core.webapi.requests.MessageCenterDeletePostBoxConversationCommandNew;
import com.ecg.messagecenter.core.webapi.requests.MessageCenterGetPostBoxCommand;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
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
import java.io.Writer;
import java.util.Arrays;

@Controller
public class PostBoxOverviewController {
    @Autowired
    private SimplePostBoxRepository postBoxRepository;

    @Autowired
    private PostBoxResponseBuilder responseBuilder;

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = MessageCenterGetPostBoxCommand.MAPPING, produces = MediaType.APPLICATION_JSON_VALUE, method = {RequestMethod.GET, RequestMethod.PUT})
    @ResponseBody
    ResponseObject<PostBoxResponse> getPostBoxByEmail(@PathVariable String email,
      @RequestParam(value = "newCounterMode", defaultValue = "false") boolean newCounterMode,
      @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
      @RequestParam(value ="page", defaultValue = "0", required = false) Integer page,
      HttpServletRequest request) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        if (markAsRead(request)) {
            postBox.resetReplies();
            postBoxRepository.markConversationsAsRead(postBox, postBox.getConversationThreads());
        }

        return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
    }

    @RequestMapping(value = MessageCenterDeletePostBoxConversationCommandNew.MAPPING, produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE)
    @ResponseBody
    ResponseObject<PostBoxResponse> removePostBoxConversationByEmailAndBulkConversationIds(@PathVariable("email") String email,
      @RequestParam(value = "ids", defaultValue = "") String[] ids,
      @RequestParam(value = "newCounterMode", defaultValue = "true") boolean newCounterMode,
      @RequestParam(value ="page", defaultValue = "0", required = false) Integer page,
      @RequestParam(value = "size", defaultValue = "50", required = false) Integer size) {
        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));

        for (String id : ids) {
            postBox.removeConversation(id);
        }

        postBoxRepository.deleteConversations(postBox, Arrays.asList(ids));
        return responseBuilder.buildPostBoxResponse(email, size, page, postBox, newCounterMode);
    }

    private boolean markAsRead(HttpServletRequest request) {
        return request.getMethod().equals(RequestMethod.PUT.name());
    }
}