package com.ecg.au.gumtree.readrepair.api;

import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.conversation.RiakReadRepairConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * Created by mdarapour.
 */
@Controller
public class ReadRepairConversationController {
    public static final String MAPPING = "/conversations/siblings/{conversationId}";

    @Autowired
    private RiakReadRepairConversationRepository readRepairConversationRepository;

    @RequestMapping(value = MAPPING, method = RequestMethod.GET)
    @ResponseBody
    ResponseObject<?> hasSiblings(@PathVariable String conversationId) {
        return ResponseObject.of(readRepairConversationRepository.hasSiblings(conversationId));
    }

    @RequestMapping(value = MAPPING, method = RequestMethod.PUT)
    @ResponseBody
    ResponseObject<?> mergeSiblings(@PathVariable String conversationId) {
        return ResponseObject.of(readRepairConversationRepository.getById(conversationId));
    }
}
