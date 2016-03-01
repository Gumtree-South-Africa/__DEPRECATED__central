package com.ecg.de.ebayk.messagecenter.util;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class MessagesDifferTest {

    private MessagesDiffer messagesDiffer;

    @Before
    public void setUp() throws Exception {
        messagesDiffer = new MessagesDiffer();
    }

    @Test
    public void diffThread1_0() throws Exception {
        String mail_0 = loadFileAsString("thread_1.0.txt");
        String mail_1 = loadFileAsString("thread_1.1.txt");

        assertEquals("und das ist die tolle antwort",
                executeDiff(mail_0, mail_1).getCleanupResult());
    }

    @Test
    public void diffThread1_1() throws Exception {
        String mail_0 = loadFileAsString("thread_1.1.txt");
        String mail_1 = loadFileAsString("thread_1.2.txt");

        assertEquals("noch mehr unfug", executeDiff(mail_0, mail_1).getCleanupResult());
    }

    @Test
    public void diffThread1_3() throws Exception {
        String mail_0 = loadFileAsString("thread_1.2.txt");
        String mail_1 = loadFileAsString("thread_1.3.txt");

        assertEquals("sub-posting", executeDiff(mail_0, mail_1).getCleanupResult());
    }

    @Test
    public void diffThread2_0() throws Exception {
        String mail_0 = loadFileAsString("thread_2.0.txt");
        String mail_1 = loadFileAsString("thread_2.1.txt");

        assertEquals("zitat unten", executeDiff(mail_0, mail_1).getCleanupResult());
    }

    @Ignore // how to handle ordering problem (user didn't reply in row)
    @Test
    public void diff() throws Exception {
        assertEquals("Einen sch\\u00f6nen Abend w\\u00fcnsche ich \n" +
                "Wollte fragen ob die dunkelgr\\u00fcne Wildlederjacke noch da ist?\n" +
                "Was f\\u00fcr ein Material ist an dem Kragen und \\u00c4rmeln?\n" +
                "Ist die Jacke sehr abgenutzt oder noch tragbar?\n" +
                "Vielen,lieben Dank Katrin", messagesDiffer.cleanupFirstMessage(loadThreadItems("plainText").get(0)));


        assertEquals("hi, ja die jacke ist noch da. Kragen/\\u00c4rmel/Futter steht dort Viskose.\n" +
                "Jacke ist tragbar.", iterateDiffOfFile2("plainText", 0, 1).get(0));


        assertEquals("Hallo Danke f\\u00fcr die Antwort;-)\n" +
                "Ist die Jacke aus echtem Wildleder? Dann nat\\u00fcrlich nicht waschbar, gell?\n" +
                "Was m\\u00fcsste ich denn inclusive Versand zahlen?\n" +
                "Liebe Gr\\u00fc\\u00dfe Katrin", iterateDiffOfFile2("plainText", 1, 2).get(0));

        assertEquals("Ja ist Wildleder, Versand w\\u00e4ren zus\\u00e4tzliche 4 EUR (siehe\n" +
                "http://www.dhl.de/de/paket/preise.html).", iterateDiffOfFile2("plainText", 2, 3).get(0));

        assertEquals("Also dann w\\u00fcrde ich die Jacke nehmen und Dir 14 Euro \\u00fcberweisen.\n" +
                "Br\\u00e4uchte halt noch die Kontodaten und Du meine Lieferanschrift.\n" +
                "Sch\\u00f6nen Abend Katrin", iterateDiffOfFile2("plainText", 3, 4).get(0));

        assertEquals("hier sind die daten:\n" +
                "konto: 123\n" +
                "blz: 50050201\n" +
                "jetzt brauche ich noch deine adresse :)", iterateDiffOfFile2("plainText", 4, 5).get(0));

        assertEquals("Hallo\n" +
                "Geht klar mit den Kontodaten. Werde diese Woche noch \\u00fcberweisen.\n" +
                "Gib Dir nochmal Bescheid wenn ich \\u00fcberwiesen habe und dann gebe ich Dir noch meine Adresse.\n" +
                "Sch\\u00f6nen Abend noch\n" +
                "Katrin", iterateDiffOfFile2("plainText", 5, 6).get(0));

        assertEquals("Mir ist aufgefallen das ich auch noch Deinen Vor - und Nachnamen f\\u00fcr die \\u00dcberweisung br\\u00e4uchte.\n" +
                "Gute Nacht", iterateDiffOfFile2("plainText", 5, 7).get(0));

        assertEquals("ups vergessen: manuel aldana", iterateDiffOfFile2("plainText", 7, 8).get(0));

        assertEquals("Hallo\n" +
                "Geld m\\u00fcsste bei Dir eingegangen sein.\n" +
                "Und hier die Adresse - FALLS - Du mit DHL versendest. K\\u00e4me mir entgegen. Also, und zwar:\n" +
                "\n" +
                "Katrin Zimmermann\n" +
                "802089282\n" +
                "Postfiliale 536\n" +
                "91224 Pommelsbrunn\n" +
                "\n" +
                "Bitte die 4 Zeilen so angeben! Vielen, lieben Dank\n" +
                "Melde mich nochmal kurz wenn die Jacke da ist ;-) Katrin"
                , iterateDiffOfFile2("plainText", 5, 9).get(0));

        assertEquals("habe das paket abgesendet", iterateDiffOfFile2("plainText", 10, 11).get(0));

        assertEquals("Hallo\n" +
                "Jacke ist gut angekommen ;-) Lieben Dank - alles Gute Katrin", iterateDiffOfFile2("plainText", 11, 12).get(0));

        assertEquals("ok sch\\u00f6n, hoffe die jacke passt (mit risiko ohne vorher anprobiert zu\n" +
                "haben).", iterateDiffOfFile2("plainText", 12, 13).get(0));

    }

    @Test
    public void thread2() throws Exception {
        assertEquals("Kacke grrtrrrrrrrr", messagesDiffer.cleanupFirstMessage(loadThreadItems("plainText2").get(0)));

        assertEquals("Test Test test", iterateDiffOfFile2("plainText2", 0, 1).get(0));

        assertEquals("Hvbnkbhhbbk", iterateDiffOfFile2("plainText2", 1, 2).get(0));

        assertEquals("Hallo", iterateDiffOfFile2("plainText2", 0, 3).get(0));

        assertEquals("Bist du das?", iterateDiffOfFile2("plainText2", 3, 4).get(0));
    }

    @Test
    public void thread3() throws Exception {
        assertEquals("Noch zu haben?", messagesDiffer.cleanupFirstMessage(loadThreadItems("plainText3").get(0)));

        assertEquals("Hallo?", iterateDiffOfFile2("plainText3", 0, 1).get(0));

        assertEquals("Schneller!", iterateDiffOfFile2("plainText3", 1, 2).get(0));

        assertEquals("Hallo das Notizbuch ist nch da!!", iterateDiffOfFile2("plainText3", 2, 3).get(0));

        assertEquals("Und?", iterateDiffOfFile2("plainText3", 3, 5).get(0));

        assertEquals("????", iterateDiffOfFile2("plainText3", 10, 11).get(0));

        assertEquals("emails schreiben f\\u00fchlt sich auf einmal so altbacken an. :-)", iterateDiffOfFile2("plainText3", 11, 12).get(0));

        assertEquals("Super. 2\\u20ac?", iterateDiffOfFile2("plainText3", 3, 4).get(0));
    }

    @Test
    public void thread4() throws Exception {
        assertEquals("Hallo,\n" +
                "\n" +
                "Ich w\\u00fcrde mich fuer den Server interessieren.\n" +
                "\n" +
                "Falls er noch da ist, hole ich ihn gern morgen oder uebermorgrn ab.\n" +
                "\n" +
                "Danke\n" +
                "\n" +
                "MfG\n" +
                "\n" +
                "Ulf", messagesDiffer.cleanupFirstMessage(loadThreadItems("plainText4").get(0)));

        assertEquals("Hallo , k\\u00f6nnen sie heute vorbei kommen zur Abholung?", iterateDiffOfFile2("plainText4", 0, 1).get(0));

        assertEquals("Hallo,\n" +
                "Ich kann gern heute Nachmittag gegen 17 Uhr vorbeikommen.\n" +
                "Wie ist die Adresse?\n" +
                "MfG\n" +
                "Ulf", iterateDiffOfFile2("plainText4", 1, 2).get(0));
    }

    @Test
    public void thread5() throws Exception {


        assertEquals("Denn darfst Du gar nicht verkaufen", messagesDiffer.cleanupFirstMessage(loadThreadItems("plainText5").get(0)));

        assertEquals("Nicht?", iterateDiffOfFile2("plainText5", 0, 1).get(0));

        assertEquals("doch klaro.", iterateDiffOfFile2("plainText5", 1, 2).get(0));

        assertEquals("Doch!", iterateDiffOfFile2("plainText5", 2, 3).get(0));

        assertEquals("Krasser Bug!!!!!", iterateDiffOfFile2("plainText5", 3, 4).get(0));

        assertEquals("Huhu", iterateDiffOfFile2("plainText5", 4, 5).get(0));

        assertEquals("Halllo, was ist denn jetzt ...", iterateDiffOfFile2("plainText5", 5, 6).get(0));
    }

    @Test
    public void thread8() throws Exception {
        assertEquals("also ich bin halb 9 beim arzt und ich sag mal ab halb 10 wer es m\\u00f6glich je nach dem wo es in schleiz ist\\u00a0",
                iterateDiffOfFile2("plainText8", 8, 9).get(0));


        assertEquals("Das ist m\\u00f6glich,kommt drauf an welche Uhrzeit.",
                iterateDiffOfFile2("plainText8", 7, 8).get(0));

        assertEquals("das wer super ah in schleiz were es m\\u00f6glich sie pers\\u00f6nlich mal anzu schauen?wir weren am montag in schleiz",
                iterateDiffOfFile2("plainText8", 6, 7).get(0));


        assertEquals("Die K\\u00fcche steht in Schleiz. Keine Sorgen,muss nicht im Dezember geholt werden,geht auch noch Anfang Januar.",
                iterateDiffOfFile2("plainText8", 5, 6).get(0));

        assertEquals("ja auf jeden fall sie sagt mir zu und wo m\\u00fcsste man sie abholen?oh also in dz werden wir das nicht schaffen am 1. 12014 w\\u00fcrde vieleicht machbar sein",
                                iterateDiffOfFile2("plainText8", 4, 5).get(0));

        assertEquals("Alle Ger\\u00e4te sind inklusive. Unser Plan ist es das wir bis 1. Januar ausgezogen sind. Genaues Datum k\\u00f6nnen wir leider noch nicht sagen, da es daran liegt wie schnell unsere neue Wohnung frei wird.\n" +
                "W\\u00fcrde die K\\u00fcche denn in Frage kommen?", iterateDiffOfFile2("plainText8", 3, 4).get(0));

        assertEquals("Hallo, \n" +
                "haben eine Einbauk\\u00fcchen-zeile anzubieten.\n" +
                "E-Herd mit Ceranfeld,K\\u00fchlschrank,Sp\\u00fclmaschine,Dunstabzugshaube,Sp\\u00fclbecken.\n" +
                "Foto auf Anfrage", messagesDiffer.cleanupFirstMessage(loadThreadItems("plainText8").get(0)));

        assertEquals("hallo h\\u00f6rt sich super an bitte um ein foto und weiter daten wie ma\\u00dfe und preis", iterateDiffOfFile2("plainText8", 0, 1).get(0));

        assertEquals("Hier sind die Bilder. Der Schrank ganz rechts in der Zeile geh\\u00f6rt original nicht dazu.\n" +
                "Breite der K\\u00fcche 2,80, mit Arbeitsplatte ist sie 3,10. Kann ja aber noch gek\\u00fcrzt werden.\n" +
                "Tiefe 60cm.\n" +
                "Angedacht waren 400\\u20ac.", iterateDiffOfFile2("plainText8", 1, 2).get(0));

        assertEquals("ok vielen dank f\\u00fcr die bilder herd und sp\\u00fchlmaschine und k\\u00fchlschrank sind dazu?bis wann m\\u00fcsste sie geholt werden und wo?", iterateDiffOfFile2("plainText8", 2, 3).get(0));


    }


    @Test
    public void thread9() throws Exception {
        assertEquals("Jimmyloschitz@googlemail.com\n" +
                "<\n" +
                ":\n" +
                "Jimmy Loschitz \\u00fcber eBay Kleinanzeigen <\n" +
                "Guten Tag, K\\u00f6nnen Sie mir bitte ein Bild von der Mutter schicken, w\\u00fcrde\n" +
                "gerne wissen wie die aussehen wenn die erwachsen sind.\n" +
                "MfG Jimmy Loschitz\n" +
                "\"", iterateDiffOfFile2("plainText9", 1, 2).get(0));


        assertEquals("Guten Tag, K\\u00f6nnen Sie mir bitte ein Bild von der Mutter schicken, w\\u00fcrde gerne wissen wie die aussehen wenn die erwachsen sind. \n" +
                "\n" +
                "MfG Jimmy Loschitz", messagesDiffer.cleanupFirstMessage(loadThreadItems("plainText9").get(0)));

        assertEquals("Und private Mailadresse. Danke Monika", iterateDiffOfFile2("plainText9", 0, 1).get(0));
    }


    @Test
    public void thread10() throws Exception {
        assertEquals("Hallo sende ihnen 345\\u20ac\n" +
                "So hat jeder was davon.\n" +
                "Bitte um Bild\n" +
                "Gesendet mit der WEB. DE iPhone App",iterateDiffOfFile2("plainText10", 4, 5).get(0));

        assertEquals("Hallo,\n" +
                "19\\u20ac per kg plus Versandkosten von 8\\u20ac ?\n" +
                "wie machen wir weiter ?\n" +
                "Gr\\u00fc\\u00dfe,\n" +
                "A. Herpe", iterateDiffOfFile2("plainText10", 3, 4).get(0));

        assertEquals("Treffen uns in der Mitte 19\\u20ac das kg.\n" +
                "Gesendet mit der WEB. DE iPhone App", iterateDiffOfFile2("plainText10", 2, 3).get(0));

        assertEquals("Hallo nehme ich.\n" +
                "Bitte um einbild.\n" +
                "Biete ihnen 18,50\\u20ac das kg an\n" +
                "Gesendet mit der WEB. DE iPhone App", iterateDiffOfFile2("plainText10", 0, 1).get(0));

        assertEquals("Hallo,\n" +
                "18,50\\u20ac sind zu wenig, m\\u00f6chte 19,50\\u20ac haben, inkl. Versand.\n" +
                "Wenn das in frage kommt, sende ich ein Bild.\n" +
                "Gr\\u00fc\\u00dfe,\n" +
                "A. Herpe", iterateDiffOfFile2("plainText10", 1, 2).get(0));
    }

    @Test
    public void thread11() throws Exception {
        assertEquals("toller link: www.other.com", iterateDiffOfFile2("plainText11", 3, 4).get(0));

        assertEquals("toller link", iterateDiffOfFile2("plainText11", 2, 3).get(0));

        assertEquals("noch ein link: www.reply-google.de", iterateDiffOfFile2("plainText11", 1, 2).get(0));

        assertEquals("ja ist alles super\n" +
                "http://kleinanzeigen.ebay.de/anzeigen/externer-link-weiterleitung.html?to=http%3A%2F%2Fwww.google.de", iterateDiffOfFile2("plainText11", 0, 1).get(0));

    }

    @Test
    public void noIndexOutOfBoundsExceptionIfLinkAtEnd() throws Exception {
        try {
            iterateDiffOfFile2("plainText13", 0, 1).get(0);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
    }

    @Test
    public void thread14() throws Exception {
        assertEquals("Das Buch ist bereits seit Freitag als B\\u00fcchersendung unterwegs\n" +
                "MfG\n" +
                "\n" +
                "\\u20ac \\u20ac", iterateDiffOfFile2("plainText14", 6, 7).get(0));
    }

    @Test
    public void thread15() throws Exception {
        assertEquals("JA, DANKESCH\\u00d6N, ABER LEIDER VERKAUFE ICH NICHT DAS BOOT\n" +
                "ICH VERMIETE LEDIGLICH DAS \\u201cSCH\\u00d6NE\\u201dHAUS!!!\n" +
                "LG\n" +
                "HEIDI", iterateDiffOfFile2("plainText15", -1, 1).get(1));
        assertEquals("Sehr sch\\u00f6nes Boot", iterateDiffOfFile2("plainText15", -1, 1).get(0));
    }

    @Test
    public void thread16() throws Exception {
        assertEquals("noch ein test sdfasdf", iterateDiffOfFile2("plainText16", 3, 4).get(0));
    }

    @Test
    public void thread17() throws Exception{
        assertEquals("Hallo, \n" +
                " \n" +
                "ich gebe schon seit l\\u00e4ngerer Zeit Online-Nachhilfe in Excel und Excel-VBA (\\u00fcberwiegend online). Da ich aus Hannover komme, kann ich Ihnen Unterst\\u00fctzung \\u00fcbers Internet anbieten. Im multimedialen Zeitalter sollte das jedoch kein Problem sein.\n" +
                "\n" +
                "\"", iterateDiffOfFile2("plainText17",-1, 0).get(0));
    }


    @Test
    @Ignore
    public void largeText() throws Exception {
        List<String> threadItems = loadThreadItems("plainTextLarge2");

        long start = System.currentTimeMillis();
        List<String> result = iterateDiffOfFile2(threadItems, -1, 134);

        for (int i = 0; i < 10; i++) {
            System.out.println(result.get(i));
        }
        System.out.println("TIME: " + (System.currentTimeMillis() - start));

    }

    @Test
    public void euroSignOffer() throws IOException {
        List<String> threadItems = loadThreadItems("plaintextEuro");
        assertEquals("Test", iterateDiffOfFile2(threadItems,-1, 1).get(0));
        threadItems = loadThreadItems("plaintextEuro1");
        assertEquals("Test", iterateDiffOfFile2(threadItems, -1, 1).get(0));
        threadItems = loadThreadItems("plaintextEuro2");
        assertEquals("Test", iterateDiffOfFile2(threadItems, -1, 1).get(0));

    }

    private List<String> iterateDiffOfFile2(List<String> threadItemsBody, int from, int to) throws IOException {

        List<String> results = new ArrayList<String>();
        if (from == -1) {

            for (int i = -1; i < threadItemsBody.size() - 1; i++) {
                if (i == -1) {
                    results.add(messagesDiffer.cleanupFirstMessage(threadItemsBody.get(i + 1)));

                } else {
                    TextDiffer.TextCleanerResult result = executeDiff(threadItemsBody.get(i), threadItemsBody.get(i + 1));
                    results.add(result.getCleanupResult());
                    if (result.getProfilingInfo() != null) {
                        System.out.printf(i + "-> " + (i + 1) + " left: %d right: %d overall: %d regex: %d ngramInit: %d ngramsScan: %d ngramsApply: %d%n",
                                threadItemsBody.get(i).length(),
                                threadItemsBody.get(i + 1).length(),
                                result.getProfilingInfo().overallTime(),
                                result.getProfilingInfo().getCleanupTimeMillis(),
                                result.getProfilingInfo().getInitNgrams(),
                                result.getProfilingInfo().getNgramScanTimeMillis(),
                                result.getProfilingInfo().getNgramApplyTimeMillis());
                    }
                }

            }
        } else {
            TextDiffer.TextCleanerResult result = executeDiff(threadItemsBody.get(from), threadItemsBody.get(to));
            results.add(result.getCleanupResult());
            if (result.getProfilingInfo() != null) {
                System.out.printf(from + "-> " + (to) + " left: %d right: %d overall: %d regex: %d ngramInit: %d ngramsScan: %d ngramsApply: %d%n",
                        threadItemsBody.get(from).length(),
                        threadItemsBody.get(to).length(),
                        result.getProfilingInfo().overallTime(),
                        result.getProfilingInfo().getCleanupTimeMillis(),
                        result.getProfilingInfo().getInitNgrams(),
                        result.getProfilingInfo().getNgramScanTimeMillis(),
                        result.getProfilingInfo().getNgramApplyTimeMillis());
            }
        }

        return results;
    }

    private List<String> iterateDiffOfFile2(String fileName, int from, int to) throws IOException {
        return iterateDiffOfFile2(loadThreadItems(fileName), from, to);
    }


    private List<String> loadThreadItems(String fileName) throws IOException {
        String s = loadFileAsString(fileName);
        Iterable<String> splitted = Splitter.on("\n").split(s);

        List<String> threadItemsBody = new ArrayList<String>();
        for (String line : splitted) {
            if (line.trim().isEmpty()) {
                continue;
            }
            line = line.replaceAll("\\\\n", "\n");
            line = line.replaceAll("\\\\r", "\r");
            line = line.replaceAll("\\\"", "\"");
            line = line.replaceAll("\"plainTextBody\": \"", "").trim();
            threadItemsBody.add(StringUtils.removeEnd(line, "\","));
        }
        return threadItemsBody;
    }

    private String loadFileAsString(String fileName) throws IOException {
        try(InputStream is = getClass().getResourceAsStream(fileName)) {
           return CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
        }

    }


    private TextDiffer.TextCleanerResult executeDiff(String mail_0, String mail_1) {
        return messagesDiffer.diff(new MessagesDiffer.DiffInput(mail_0, "a:a", "1:1"), new MessagesDiffer.DiffInput(mail_1, "b:b", "2:2"));
    }


}
