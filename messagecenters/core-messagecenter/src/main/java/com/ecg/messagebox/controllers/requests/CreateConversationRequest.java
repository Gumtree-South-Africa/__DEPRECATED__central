package com.ecg.messagebox.controllers.requests;

import java.util.HashSet;
import java.util.Set;

public class CreateConversationRequest {
    public String subject;
    public Set<String> participantIds = new HashSet<>();
}
