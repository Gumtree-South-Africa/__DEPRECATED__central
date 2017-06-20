package com.ecg.messagebox.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MessageTypeTest {

    @Test
    public void shouldReturnMessageTypeByString() {
        assertEquals(MessageType.ASQ, MessageType.get("asq"));
        assertNull(MessageType.get(null));
    }

    @Test
    public void shouldReturnEmailByDefaultWhenValueIsNull() {
        assertEquals(MessageType.ASQ, MessageType.getWithEmailAsDefault("asq"));
        assertEquals(MessageType.EMAIL, MessageType.getWithEmailAsDefault(null));
    }
}
