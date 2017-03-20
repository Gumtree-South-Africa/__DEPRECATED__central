package com.ecg.messagebox.controllers.responses.converters;

import com.ecg.messagebox.controllers.responses.ParticipantResponse;
import com.ecg.messagebox.model.Participant;
import com.ecg.messagebox.model.ParticipantRole;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ParticipantResponseConverterTest {

    private ParticipantResponseConverter responseConverter;

    @Before
    public void setup() {
        responseConverter = new ParticipantResponseConverter();
    }

    @Test
    public void toParticipantResponse() {
        Participant participant = new Participant("123", "name", "email", ParticipantRole.BUYER);
        ParticipantResponse expected = new ParticipantResponse("123", "name", "email", "buyer");
        ParticipantResponse actual = responseConverter.toParticipantResponse(participant);
        assertThat(actual, is(expected));
    }

    @Test
    public void toParticipantResponse_noNamePresent() {
        Participant participant = new Participant("123", null, "email", ParticipantRole.BUYER);
        ParticipantResponse expected = new ParticipantResponse("123", "", "email", "buyer");
        ParticipantResponse actual = responseConverter.toParticipantResponse(participant);
        assertThat(actual, is(expected));
    }
}