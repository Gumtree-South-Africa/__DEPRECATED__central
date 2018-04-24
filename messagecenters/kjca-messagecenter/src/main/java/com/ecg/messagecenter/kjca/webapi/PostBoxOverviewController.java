package com.ecg.messagecenter.kjca.webapi;

import com.ecg.messagecenter.kjca.persistence.block.ConversationBlockRepository;
import com.ecg.messagecenter.persistence.simple.PostBox;
import com.ecg.messagecenter.persistence.simple.PostBoxId;
import com.ecg.messagecenter.persistence.simple.SimplePostBoxRepository;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponse;
import com.ecg.messagecenter.kjca.webapi.responses.PostBoxResponseBuilder;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
class PostBoxOverviewController {

    private final SimplePostBoxRepository postBoxRepository;
    private final PostBoxResponseBuilder responseBuilder;

    @Autowired
    public PostBoxOverviewController(
            SimplePostBoxRepository postBoxRepository,
            ConversationBlockRepository conversationBlockRepository,
            @Value("${replyts.maxConversationAgeDays:180}") int maxAgeDays) {
        this.postBoxRepository = postBoxRepository;
        this.responseBuilder = new PostBoxResponseBuilder(conversationBlockRepository, maxAgeDays);
    }

    @GetMapping("/postboxes/{email:.+}")
    ResponseObject<PostBoxResponse> getPostBoxByEmail(
            @PathVariable String email,
            @RequestParam(value = "size", defaultValue = "50", required = false) Integer size,
            @RequestParam(value = "page", defaultValue = "0", required = false) Integer page,
            @RequestParam(value = "role", required = false) ConversationRole role) {

        PostBox postBox = postBoxRepository.byId(PostBoxId.fromEmail(email));
        return responseBuilder.buildPostBoxResponse(email, size, page, role, postBox);
    }
}
