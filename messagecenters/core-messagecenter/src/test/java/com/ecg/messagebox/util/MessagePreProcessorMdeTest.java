package com.ecg.messagebox.util;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MessagePreProcessorMdeTest extends MessagePreProcessorTest {
    private static final List<String> PATTERNS = Arrays.asList(
      "\\n.*(a|b)-.*?@mail.mobile.de.*\\n",
      "(Aan|To)\\s?:.*?@.*?",
      "(Subject|Onderwerp|Betreff)\\s?:.*?",
      "(Date|Datum)\\s?:.*?",
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
      "[0-9][0-9][0-9]?[0-9]?[./-].*buyer-.*@mail.mobile.de", // google client TOFU template
      "[0-9][0-9][0-9]?[0-9]?[./-].*seller-.*@mail.mobile.de", // google client TOFU template
      "[0-9][0-9][0-9]?[0-9]?[./-].*buyer-.*@kontakt.mobile.de", // google client TOFU template
      "[0-9][0-9][0-9]?[0-9]?[./-].*seller-.*@kontakt.mobile.de" // google client TOFU template
    );

    @Test
    public void realAnswerTest() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer1.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer1_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer2Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer2.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer2_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer3Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer3.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer3_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }


    @Test
    public void realAnswer4Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer4_google_client.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer4_google_client_cut.txt");

        cutAndCompare(PATTERNS, msg, expected);
    }

    @Test
    public void realAnswer5Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer5.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/emailAnswer5_cut.txt");

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
}