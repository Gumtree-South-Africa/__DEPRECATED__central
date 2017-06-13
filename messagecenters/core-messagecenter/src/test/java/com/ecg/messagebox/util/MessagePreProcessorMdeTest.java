package com.ecg.messagebox.util;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MessagePreProcessorMdeTest extends MessagePreProcessorTest {
    private static final List<String> PATTERNS = Arrays.asList(
            // Am 7. Juni 2017 um 09:39 schrieb <seller-pdgfwlf32gq9r@kontakt.mobile.de>:
            "Am [0-3]?[0-9]\\. (Ja|Fe|Mä|Ap|Ma|Ju|Au|Se|Ok|No|De)[a-z]+ 2[0-9][0-9][0-9] um [0-2]?[0-9]:[0-5]?[0-9] schrieb (<)?(seller|buyer)-.*@[a-z]+.mobile.de(>|\\s)?:.*\\n",
            //On Thu, Jun 1, 2017 at 1:32 PM
            "On (Mo|Tu|We|Th|Fr|Sa|Su)[a-z]+, (Ja|Fe|Ma|Ap|Ma|Ju|Au|Se|Oc|No|De)[a-z]+ [0-3]?[0-9]\\, 2[0-9][0-9][0-9] at [0-2]?[0-9]:[0-5]?[0-9].*\\n",
            //Gesendet: Donnerstag, 01. Juni 2017 um 13:56 Uhr"
            "(Gesendet): (Mo|Di|Mi|Do|Fr|Sa|So)[a-z]+, [0-3]?[0-9]\\. (Ja|Fe|Mä|Ap|Ma|Ju|Au|Se|Ok|No|De)[a-z]+ 2[0-9][0-9][0-9] um [0-2]?[0-9]:[0-5]?[0-9].*\\n",
            "(Aan|To)\\s?:.*?@.*?",
            "(Subject|Onderwerp|Betreff)\\s?:.*?",
            "\\w*(?<!=[?=\\n])(Date|Datum)\\s?:.*?",
            "\\n.*<[^<>\\s]+@gmail.[^<>\\s]+>.*\\n",
            "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *(?:</b>)? *<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
            "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *(?:</b>)? *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
            "<span[^>]*>(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *</span *>[^<>]*(?:<[:a-z]+[^>]*>){0,2}[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>){0,2}",
            "<b><span[^>]*>(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *</span *></b> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
            "<span[^>]*><b>(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *</b></span *> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
            "\\b(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
            "\\b(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *([^<>\\s]+ +){1,6}(<|&lt;)?[^<>\\s]+@[^<>\\s]+((<|&lt;)[^<>\\s]+@[^<>\\s]+(>|&gt;))?(>|&gt;)?",
            "\\b(From|To|Sender|Receiver|Van|Aan|Von|An|Gesendet) *: *([^<>\\s]+ +){0,5}([^<>\\s]+)(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
            "Op.{10,25}schrieb[^<]{5,60}<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
            "Op.{10,25}schrieb[^<]{5,60}(<|&lt;)?\\s*[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
            "Am [0-9][0-9][0-9]?[0-9]?[./-].* schrieb.*",
            "On [0-9][0-9][0-9]?[0-9]?[./-].* wrote.*",
            "\\s+<b>*Gesendet*:*</b>",
            "\\n.*(s|b)-.*?@[a-z]+.mobile.de.*\\n",
            "2[0-9][0-9][0-9][./-][0-9]?[0-9][./-][0-9]?[0-9] [0-2]?[0-9]:[0-5]?[0-9] .*(seller|buyer)-.*@[a-z]+.mobile.de.*",
            "[0-9][0-9][0-9]?[0-9]?[./-].*(seller|buyer)-.*@[a-z]+.mobile.de.*\\n"
    );

    @Test
    public void realAnswerTest() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer1.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer1_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer2Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer2.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer2_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer3Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer3.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer3_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }


    @Test
    public void realAnswer4Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer4_google_client.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer4_google_client_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer5Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer5.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer5_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer6Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer6.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer6_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer7Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer7.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer7_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer8Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer8.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer8_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer9Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer9.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mde/emailAnswer9_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void removeEmptyBrTags() throws IOException {
        cutAndCompare(PATTERNS, "no slash<br>", "no slash");
        cutAndCompare(PATTERNS, "with slash<br/>", "with slash");
        cutAndCompare(PATTERNS, "with slash and spaces<br   />", "with slash and spaces");
        cutAndCompare(PATTERNS, "no slash many spaces<br   >", "no slash many spaces");
        cutAndCompare(PATTERNS, "no slash many spaces<br class=\"any\"  >", "no slash many spaces");
    }

    @Test
    public void outlook12Test() throws Exception {

        String msg = loadFileAsString("/com/ecg/messagecenter/util/outlook12Message");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/outlook12Message_answer");

        cutAndCompare(PATTERNS, msg, expected);
    }
}