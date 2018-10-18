package com.ecg.replyts.core.api.model.conversation;

import com.ecg.comaas.events.Conversation;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class ProtoMapperTest {

    @Test
    public void messageTransportToProtoTest() {
        Arrays.stream(MessageTransport.values())
                .forEach(transport -> assertEquals(ProtoMapper.messageTransportToProto(transport).name(), transport.name()));
    }

    @Test
    // though this transformation ( proto -> MessageTransport) is never used in our application, I prefer to have both
    // enums in sync to be sure that this application's business logic has not to be changed if a new protobuf enum
    // value is added
    public void protoToMessageTransportTest() {
        Arrays.stream(Conversation.Transport.values())
                .filter(transport -> transport != Conversation.Transport.UNRECOGNIZED)
                .forEach(transport -> MessageTransport.valueOf(transport.name()));
    }
}