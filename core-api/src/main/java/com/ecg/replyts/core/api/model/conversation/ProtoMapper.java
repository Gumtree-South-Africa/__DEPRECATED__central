package com.ecg.replyts.core.api.model.conversation;

import com.ecg.comaas.events.Conversation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ProtoMapper {

    private static Map<MessageTransport, Conversation.Transport> transportMapper = new HashMap<>();

    static {
        Arrays.stream(MessageTransport.values())
                .forEach(transfer -> transportMapper.put(transfer, Conversation.Transport.valueOf(transfer.name())));
    }

    public static Conversation.Transport messageTransportToProto(MessageTransport transport) {
        return transportMapper.get(transport);
    }

}
