package com.ecg.messagebox.controllers.requests;

import com.ecg.messagebox.model.Participant;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateConversationRequest {

    @NotNull(message = "Conversation subject text cannot be empty")
    public String subject;

    @NotNull(message = "List of participants cannot be empty")
    public List<Participant> participants = new ArrayList<>();

    public String title;

    public String imageUrl;

    public Map<String, String> metadata = new HashMap<>();
}
