package com.ecg.messagebox.controllers.requests;

import com.ecg.messagebox.model.Message;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

public class EmptyConversationRequest {

    @NotNull
    @NotEmpty
    private String senderId;

    @NotNull
    @NotEmpty
    private String adId;

    @NotNull
    @NotEmpty
    private String adTitle;

    @Valid
    @Size(min = 2, max = 2)
    @NotNull
    private Map<ParticipantRole, Participant> participants = new HashMap();

    @Valid
    @NotNull
    private Message message;

    @Valid
    @Size(min = 2)
    @NotNull
    private Map<String, String> customValues;

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getAdTitle() {
        return adTitle;
    }

    public void setAdTitle(String adTitle) {
        this.adTitle = adTitle;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }


    public Map<ParticipantRole, Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<ParticipantRole, Participant> participants) {
        this.participants = participants;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Map<String, String> getCustomValues() {
        return customValues;
    }

    public void setCustomValues(Map<String, String> customValues) {
        this.customValues = customValues;
    }
}
