package com.ecg.comaas.mde.postprocessor.sms;

import org.junit.Assert;
import org.junit.Test;

public class ContactMessageSmsServiceTest {

    @Test
    public void testSenderParsing() {
        String defaultSender1 = ContactMessageSmsService.parseSender(null);
        String defaultSender2 = ContactMessageSmsService.parseSender("");
        String defaultSender3 = ContactMessageSmsService.parseSender("bla");

        Assert.assertEquals(ContactMessageSmsService.SENDER, defaultSender1);
        Assert.assertEquals(ContactMessageSmsService.SENDER, defaultSender2);
        Assert.assertEquals(ContactMessageSmsService.SENDER, defaultSender3);

        String csSender = ContactMessageSmsService.parseSender("cs_CZ");
        Assert.assertEquals(ContactMessageSmsService.SENDER, csSender);

        String czSender = ContactMessageSmsService.parseSender("cz_CZ");
        Assert.assertEquals(ContactMessageSmsService.SENDER, czSender);

        String ruSender = ContactMessageSmsService.parseSender("ru_RU");
        Assert.assertEquals(ContactMessageSmsService.SENDER, ruSender);

        String deSender = ContactMessageSmsService.parseSender("de_DE");
        Assert.assertEquals("Abs.", deSender);

        String esSender = ContactMessageSmsService.parseSender("es_ES");
        Assert.assertEquals("Remitente", esSender);

        String frSender = ContactMessageSmsService.parseSender("fr_FR");
        Assert.assertEquals("Expéditeur", frSender);

        String itSender = ContactMessageSmsService.parseSender("it_IT");
        Assert.assertEquals("Mittente", itSender);

        String plSender = ContactMessageSmsService.parseSender("pl_PL");
        Assert.assertEquals("Wysyłający", plSender);

        String roSender = ContactMessageSmsService.parseSender("ro_RO");
        Assert.assertEquals("Expeditor", roSender);
    }
}
