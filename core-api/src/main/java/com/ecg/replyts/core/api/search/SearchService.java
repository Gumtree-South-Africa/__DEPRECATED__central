package com.ecg.replyts.core.api.search;

import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessageGroupPayload;
import com.ecg.replyts.core.api.webapi.commands.payloads.SearchMessagePayload;

public interface SearchService {

    RtsSearchResponse search(SearchMessagePayload searchMessageCommand);

    RtsSearchGroupResponse search(SearchMessageGroupPayload searchMessageCommand);
}
