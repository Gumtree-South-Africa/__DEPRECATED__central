package com.ecg.messagecenter.it.cleanup;

import com.ecg.messagecenter.it.chat.Template;
import com.ecg.messagecenter.it.cleanup.TextCleaner;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by jaludden on 10/12/15.
 */
public class ItalianTextCleanerTest {

    @Before
    public void setUp() {
        TextCleaner.setInstance(new TextCleaner.KijijiTextCleaner());
    }

    @After
    public void tearDown() {
        TextCleaner.setInstance(new TextCleaner.GumtreeTextCleaner());
    }

    @Test
    public void testGenerateAndCleanupFromSellerToBuyer() throws Exception {
        String userMessage = "This is the user message";
        Map<String, Object> params = new HashMap<>();
        params.put("message", userMessage);
        params.put("from", "A buyer");
        params.put("type", "annuncio");
        params.put("greating", "Ecco cosa ti ha scritto:");
        params.put("toSeller", false);
        String messageWithTemplate = new Template("http://siteurl", "http://cdnUrl")
                .createPostReplyMessage(params);
        assertThat(TextCleaner.cleanupText(messageWithTemplate), is(userMessage));
        assertEquals(messageWithTemplate,
                contentOf("kijiji/message_with_template_seller_to_buyer.txt"));
    }

    @Test
    public void testGenerateAndCleanupFromBuyerToSeller() throws Exception {
        String userMessage = "This is the user message\nBye.";
        Map<String, Object> params = new HashMap<>();
        params.put("message", userMessage);
        params.put("from", "A buyer");
        params.put("type", "annuncio");
        params.put("greating", "Ecco cosa ti ha scritto:");
        params.put("toSeller", true);
        String messageWithTemplate = new Template("http://siteurl", "http://cdnUrl")
                .createPostReplyMessage(params);
        assertEquals(messageWithTemplate,
                contentOf("kijiji/message_with_template_buyer_to_seller.txt"));
    }

    private String contentOf(String file) throws IOException {
        InputStream st = getClass().getResourceAsStream(file);
        StringWriter writer = new StringWriter();
        IOUtils.copy(st, writer);
        return writer.toString().replace("{year}", new SimpleDateFormat("yyyy").format(new Date()));
    }

    @Test
    public void testMessageWithGreaterThenOnStartOfLine() {
        String userMessage = "This is the user message";
        String messageWithTemplate = new Template("http://siteurl", "http://cdnUrl")
                .createPostReplyMessage(
                        Collections.singletonMap("message", ">" + userMessage));
        assertThat(TextCleaner.cleanupText(messageWithTemplate), is("&gt;" + userMessage));
    }

    @Test
    public void testMessageWithBreaksShouldBeRepresentedAsNewLine() {
        String message = "Mi illumino<br/>di immenso!<br />Ciao<BR>- Ungaretti";
        String messageWithTemplate = new Template("http://siteurl", "http://cdnUrl")
                .createPostReplyMessage(Collections.singletonMap("message", message));
        assertThat(TextCleaner.cleanupText(messageWithTemplate),
                is("Mi illumino\ndi immenso!\nCiao\n- Ungaretti"));
    }

    @Test
    public void testFirstMessageFromZapi() {
        String zapiMessage =
                "    Ciao!\n        Ivan Coppa ha risposto al tuo annuncio \"Vi insegno ad usare i FOREACH\".\n"
                        +
                        "    Ecco il suo messaggio:\n\n" +
                        "----------\n" +
                        "    Bello il centro massaggi!!\nBravi ragazzi :)\n" +
                        "----------\n\n" +
                        "Rispondendo a questa mail contatterai l'utente direttamente.\n\n"
                        +
                        "==> Per tutelare la tua PRIVACY ti consigliamo di utilizzare i sistemi\n"
                        +
                        "    di messaggistica che Kijiji ti offre. Vuoi saperne di più?\n"
                        +
                        "    http://env2.kijiji-qa.it/guida-miei-messaggi\n\n" +
                        "==> Se vuoi rivedere il tuo annuncio: http://env2.kijiji-qa.it/post/29245209?\n"
                        +
                        "==> Se invece l'hai già venduto e vuoi eliminarlo: http://env2.kijiji-qa.it/miei-annunci\n\n"
                        +
                        "==> SUGGERIMENTO DA KIJIJI\n    Fai sempre attenzione a link che aprono siti diversi da kijiji.it e se l'attività\n"
                        +
                        "    di questo utente ti sembra sospetta, o il linguaggio non è appropriato, segnalacelo\n"
                        +
                        "    http://supporto.kijiji.it/customer/portal/emails/new\n\n"
                        +
                        "==> NOVITA'!\n" +
                        "    * Hai già un account su Kijiji?\n" +
                        "    Allora puoi comunicare in tempo reale con Ivan Coppa e tutti gli altri utenti\n"
                        +
                        "    in modo ancora più veloce, intuitivo e affidabile!\n" +
                        "    Accedi ai tuoi messaggi dal web: http://env2.kijiji-qa.it/miei-annunci\n"
                        +
                        "    Oppure scarica gratis la nostra app: https://app.adjust.com/nfhqnm\n"
                        +
                        "    * Non hai ancora un account? Crealo ora, è facile e gratuito!\n"
                        +
                        "    http://env2.kijiji-qa.it/miei-annunci/registrati\n\n" +
                        "----------\n" +
                        "Se hai problemi ad accedere, copia e incolla i link nella barra di navigazione.\n"
                        +
                        "----------\n" +
                        "Il team di Kijiji\n\n\n" +
                        "-------------------------------------------------------------------\n"
                        +
                        "Copyright © 2016 eBay International AG. Tutti i diritti riservati. I marchi registrati ed i segni\n"
                        +
                        "distintivi sono di proprietà dei rispettivi titolari. L'uso di questo sito web implica\n"
                        +
                        "l'accettazione delle Condizioni d'uso e delle Regole sulla privacy di Marktplaats BV.\n\n";
        assertThat(TextCleaner.cleanupText(zapiMessage),
                is("Bello il centro massaggi!!\nBravi ragazzi :)"));
    }

    @Test
    public void cleanupText() throws Exception {
        File mailFolder = new File(getClass().getResource("kijiji").getFile());
        Map<String, String> expected = Maps.newHashMap();
        FileInputStream fin;
        long start, end, duration;
        String fileName, result;

        File[] mails = mailFolder.listFiles((dir, name) -> name.endsWith("eml"));
        File[] texts = mailFolder.listFiles((dir, name) -> name.endsWith("txt"));

        for (File text : texts) {
            expected.put(fileName(text.getName()), IOUtils.toString(new FileInputStream(text), Charset.defaultCharset()));
        }

        for (File f : mails) {
            fin = new FileInputStream(f);
            fileName = fileName(f.getName());
            try {
                Mail mail = Mails.readMail(ByteStreams.toByteArray(fin));
                List<String> parts = mail.getPlaintextParts();
                start = System.currentTimeMillis();
                result = TextCleaner.cleanupText(parts.get(0));
                end = System.currentTimeMillis();
                duration = end - start;
                assertTrue("Long running regex, Length: " + result.length() + " Time: " + duration
                        + " File:[" + fileName + "]", duration < 30 * 10);
                if (expected.containsKey(fileName)) {
                    assertEqualsIgnoreLineEnding("File '" + f.getAbsolutePath() + "'", expected.get(fileName), result);
                }
            } catch (Exception e) {
                throw new RuntimeException("Mail " + f.getAbsolutePath() + " not parseable", e);
            } finally {
                fin.close();
            }
        }
    }

    private static void assertEqualsIgnoreLineEnding(String message, String expected, String actual) {
        assertThat(message, actual.trim(), is(expected.trim()));
    }

    private String fileName(String fileName) {
        return fileName.replaceAll("\\..*", "");
    }
}
