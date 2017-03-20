package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.Participant;
import org.springframework.stereotype.Component;

@Component
public class ParticipantResponseConverter {

    public ParticipantResponse toParticipantResponse(Participant participant) {
        return new ParticipantResponse(
                participant.getUserId(),
                participant.getName(),
                participant.getEmail(),
                participant.getRole().getValue()
        );
    }
}