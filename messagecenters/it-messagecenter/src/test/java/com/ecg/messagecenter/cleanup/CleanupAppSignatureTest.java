package com.ecg.messagecenter.cleanup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class) public class CleanupAppSignatureTest {

    private String expected;
    private String dirtyPlaintTextMessage;

    public CleanupAppSignatureTest(String expected, String dirtyPlaintTextMessage) {
        this.expected = expected;
        this.dirtyPlaintTextMessage = dirtyPlaintTextMessage;
    }

    @Parameterized.Parameters public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {{"Questo non è un messaggio.",
                        "Questo non è un messaggio.\r\nSent from Mail.Ru app for iOS\r\n\r\nVuoi saperne di più? Clicca qui! [...] Scarica gratis la nostra app."},
                        {"Questo non è un messaggio.",
                                        "Questo non è un messaggio.\r\nSent from iPhone"},
                        {"Questo non è un messaggio.",
                                        "Questo non è un messaggio.\r\nInviato da iPhone"},
                        // back-quoted dashes
                        {"Questo non è un messaggio.",
                                        "Questo non è un messaggio.\r\n--\r\nInviato da Libero Mail per Android"},
                        // below threshold
                        {"Questo non è un messaggio.",
                                        "Questo non è un messaggio.\r\n--\r\nPino Scotto\r\nhttp://pinoscotto.it"},
                        // over the threshold
                        {"Questo non è un messaggio.\n--\nLino Banfi\nlino@banfi.it\nE questo me lo devo tenere.",
                                        "Questo non è un messaggio.\r\n--\r\nLino Banfi\r\nlino@banfi.it\r\nE questo me lo devo tenere.\r\n"},
                        //
                        {"Salve le allego copia bonifico\nSaluti",
                                        " \n\nSalve le allego copia bonifico\nSaluti\nInviato da Samsung Mobile. ",},});
    }

    @Test public void itShouldExtractTheRightMessage() {
        assertThat(new TextCleaner.KijijiTextCleaner()
                        .internalCleanupText(this.dirtyPlaintTextMessage), is(this.expected));
    }
}
