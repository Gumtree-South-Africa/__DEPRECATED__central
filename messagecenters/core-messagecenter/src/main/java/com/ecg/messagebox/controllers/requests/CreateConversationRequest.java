package com.ecg.messagebox.controllers.requests;

import com.ecg.messagebox.model.Participant;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CreateConversationRequest {

    @NotNull(message = "Conversation subject text cannot be empty")
    public String subject;

    @NotNull(message = "List of participants cannot be empty")
    public List<Participant> participants = new ArrayList<>();
}
