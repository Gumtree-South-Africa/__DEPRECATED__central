package com.ecg.messagebox.util;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class MessagePreProcessorMpTest extends MessagePreProcessorTest {
    private static final List<String> PATTERNS_MP = Arrays.asList(
      "\\n.*(a|b)-.*?@mail.marktplaats.nl.*\\n",
      "(Aan|To)\\s?:.*?@.*?",
      "(Subject|Onderwerp)\\s?:.*?",
      "(Date|Datum)\\s?:.*?",
      "\\n.*<[^<>\\s]+@gmail.[^<>\\s]+>.*\\n",
      "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)? *<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
      "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)? *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
      "<span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *>[^<>]*(?:<[:a-z]+[^>]*>){0,2}[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>){0,2}",
      "<b><span[^>]*>(From|To|Sender|Receiver|Van|Aan) *: *</span *></b> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
      "<span[^>]*><b>(From|To|Sender|Receiver|Van|Aan) *: *</b></span *> *(?:<[:a-z]+[^>]*>)?[^<>\\s]+@[^<>\\s]+(?:</[:a-z]+>)?",
      "\\b(From|To|Sender|Receiver|Van|Aan) *: *(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
      "\\b(From|To|Sender|Receiver|Van|Aan) *: *([^<>\\s]+ +){1,6}(<|&lt;)?[^<>\\s]+@[^<>\\s]+((<|&lt;)[^<>\\s]+@[^<>\\s]+(>|&gt;))?(>|&gt;)?",
      "\\b(From|To|Sender|Receiver|Van|Aan) *: *([^<>\\s]+ +){0,5}([^<>\\s]+)(<|&lt;)?[^<>\\s]+@[^<>\\s]+(>|&gt;)?",
      "\\b(?:<b>)?(From|To|Sender|Receiver|Van|Aan) *: *(?:</b>)?[^<>]+(&lt;)[^<>\\s]+@[^<>\\s]+(&gt;)",
      "Op.{10,25}schreef[^<]{5,60}<a[^>]+href=\"mailto:[^\">]+@[^\">]+\"[^>]*>[^<]*</a",
      "Op.{10,25}schreef[^<]{5,60}(<|&lt;)?\\s*[^<>\\s]+@[^<>\\s]+(>|&gt;)?"
    );

    @Test
    public void realAnswerTest() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mp/emailAnswer1.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mp/emailAnswer1_cut.txt");

        cutAndCompare(PATTERNS_MP, msg, expected);
    }

    @Test
    public void realAnswer2Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mp/emailAnswer2.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mp/emailAnswer2_cut.txt");

        cutAndCompare(PATTERNS_MP, msg, expected);
    }

    @Test
    public void realAnswer3Test() throws IOException {
        String msg = loadFileAsString("/com/ecg/messagecenter/util/mp/emailAnswer3.txt");
        String expected = loadFileAsString("/com/ecg/messagecenter/util/mp/emailAnswer3_cut.txt");

        cutAndCompare(PATTERNS_MP, msg, expected);
    }
}