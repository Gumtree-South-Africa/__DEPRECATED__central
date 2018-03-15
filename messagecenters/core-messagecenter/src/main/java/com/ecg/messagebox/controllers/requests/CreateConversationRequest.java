package com.ecg.messagebox.controllers.requests;

import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

public class CreateConversationRequest {

    @NotNull(message = "Conversation subject text cannot be null")
    public String subject;

    @NotNull(message = "Set of participants cannot be null")
    public Set<String> participantIds = new HashSet<>();
}
