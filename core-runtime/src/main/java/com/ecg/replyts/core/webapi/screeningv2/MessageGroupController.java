package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.search.RtsSearchGroupResponse;
import com.ecg.replyts.core.api.search.RtsSearchResponse;
import com.ecg.replyts.core.api.search.SearchService;
import com.ecg.replyts.core.api.webapi.commands.SearchMessageGroupCommand;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.envelope.PaginationInfo;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.MessageGroupRts;
import com.ecg.replyts.core.api.webapi.model.MessageRts;
import com.ecg.replyts.core.api.webapi.model.imp.MessageGroupRtsRest;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Controller
public class MessageGroupController {
    private final DomainObjectConverter converter;
    private final SearchService searchService;

    @Autowired
    public MessageGroupController(
            DomainObjectConverter converter,
            SearchService searchService
    ) {
        this.converter = converter;
        this.searchService = searchService;
    }


    /**
     * Performs a message search and groups the results according to the request.
     * Search command must be described in the post payload.
     */
    @RequestMapping(value = SearchMessageGroupCommand.MAPPING, consumes = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseObject<?> searchMessages(@RequestBody SearchMessageGroupPayload command) {
        RtsSearchGroupResponse searchResponse = searchService.search(command);
        Map<String, RtsSearchResponse> bareMessageGroups = searchResponse.getMessageGroups();

        List<MessageGroupRts> realizedGroups = new ArrayList<>(bareMessageGroups.size());
        for (Map.Entry<String, RtsSearchResponse> entry : bareMessageGroups.entrySet()) {
            RtsSearchResponse value = entry.getValue();
            List<MessageRts> fetchedMessages = converter.convertFromSearchResults(value.getResult());
            realizedGroups.add(new MessageGroupRtsRest(entry.getKey(), fetchedMessages, new PaginationInfo(value.getOffset(), value.getCount(), value.getTotal())));
        }

        return ResponseObject.of(realizedGroups);
    }
}
