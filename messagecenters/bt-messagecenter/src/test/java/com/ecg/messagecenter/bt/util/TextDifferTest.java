package com.ecg.messagecenter.bt.util;

import com.ecg.messagecenter.bt.util.MessagesDiffer;
import com.ecg.messagecenter.bt.util.TextDiffer;
import org.junit.Test;

import static com.ecg.messagecenter.bt.util.TextDiffer.NGram;
import static com.ecg.messagecenter.bt.util.TextDiffer.NGramMatch;
import static junit.framework.Assert.assertEquals;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class TextDifferTest {


    private static final int DOESNT_MATTER = 0;

    @Test
    public void textCleaner() throws Exception {
        TextDiffer cleaner = createTextCleaner("das ist   ein\n\n text", 2);

        assertEquals(4, cleaner.getNgramsOfText().size());
        assertEquals(new NGram("das", " ", DOESNT_MATTER), cleaner.getNgramsOfText().get(0));
        assertEquals(new NGram("ist", " ", DOESNT_MATTER), cleaner.getNgramsOfText().get(1));
        assertEquals(new NGram("ein", "\n", DOESNT_MATTER), cleaner.getNgramsOfText().get(2));
        assertEquals(new NGram("text", "", DOESNT_MATTER), cleaner.getNgramsOfText().get(3));
    }

    @Test
    public void findMatch() throws Exception {
        TextDiffer cleaner = createTextCleaner("das ist   ein\n\n text", 2);

        cleaner.findMatch(new NGram("das", " ", DOESNT_MATTER));

        assertEquals(1, cleaner.getIndexedMatches().size());
        assertEquals(new NGramMatch(new NGram("das", " ", DOESNT_MATTER), 0), cleaner.getIndexedMatches().keySet().iterator().next());
    }

    @Test
    public void cleanupMatches() throws Exception {
        TextDiffer cleaner = createTextCleaner("das ist   ein\n\n text", 2);

        cleaner.findMatch(new NGram("das", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("ist", " ", DOESNT_MATTER));

        assertEquals("ein\ntext", cleaner.cleanupMatches().getCleanupResult());
    }


    @Test
    public void handleInsertedLinebreaks() throws Exception {
        TextDiffer cleaner = createTextCleaner("das\n text", 1);

        cleaner.findMatch(new NGram("das", " ", DOESNT_MATTER));

        assertEquals("text", cleaner.cleanupMatches().getCleanupResult());
    }

    @Test
    public void noCleanup() throws Exception {
        TextDiffer cleaner = createTextCleaner("kein cleanup", 2);

        assertEquals("kein cleanup", cleaner.cleanupMatches().getCleanupResult());
    }

    @Test
    public void multipleOccurences() throws Exception {
        TextDiffer cleaner = createTextCleaner("das ist ein füllwort ist super", 2);

        cleaner.findMatch(new NGram("ist", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("ein", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("ist", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("super", "", DOESNT_MATTER));

        assertEquals("das füllwort", cleaner.cleanupMatches().getCleanupResult());
    }

    @Test
    public void greedySnipAway1() throws Exception {
        TextDiffer cleaner = createTextCleaner("das ist ein text > zitate sind > blöd", 2);

        cleaner.findMatch(new NGram("zitate", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("sind", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("blöd", "", DOESNT_MATTER));

        assertEquals("das ist ein text", cleaner.cleanupMatches().getCleanupResult());
    }

    @Test
    public void greedySnipAway2() throws Exception {
        TextDiffer cleaner = createTextCleaner("das ist ein >> zitat toll", 2);

        cleaner.findMatch(new NGram("zitat", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("toll", "", DOESNT_MATTER));

        assertEquals("das ist ein", cleaner.cleanupMatches().getCleanupResult());
    }

    @Test
    public void resetOnNonFound() throws Exception {
        TextDiffer cleaner = createTextCleaner("Halllo, was ist denn jetzt ...\n" +
                "\n" +
                " >   \n" +
                " >  \n" +
                " >  \n" +
                " >   Nicht da?\n" +
                " >  \n", 2);

        cleaner.findMatch(new NGram("Huhu", "", DOESNT_MATTER));
        cleaner.findMatch(new NGram("Nicht", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("da?", "\n", DOESNT_MATTER));

        assertEquals("Halllo, was ist denn jetzt ...", cleaner.cleanupMatches().getCleanupResult());
    }

    @Test
    public void doNotRemoveOnBoundaryDeletesSection() throws Exception {
        TextDiffer cleaner = createTextCleaner("das IGNORE-WEG-DAMIT sind", 2);

        cleaner.findMatch(new NGram("IGNORE-WEG-DAMIT", " ", DOESNT_MATTER));

        assertEquals("das IGNORE-WEG-DAMIT sind", cleaner.cleanupMatches().getCleanupResult());
    }

    @Test
    public void followOrderOfMatches() throws Exception {
        TextDiffer cleaner = createTextCleaner("das WIRKLICH IGNORE-WEG-DAMIT sind WIRKLICH IGNORE-WEG-DAMIT mehr", 2);

        cleaner.findMatch(new NGram("WIRKLICH", " ", DOESNT_MATTER));
        cleaner.findMatch(new NGram("IGNORE-WEG-DAMIT", " ", DOESNT_MATTER));

        assertEquals("das WIRKLICH IGNORE-WEG-DAMIT sind mehr", cleaner.cleanupMatches().getCleanupResult());
    }


    private TextDiffer createTextCleaner(String text, int succedingMatchesToMatchPhrase) {
        return new TextDiffer(new MessagesDiffer.DiffInput(text, "a:a", "1:1"), succedingMatchesToMatchPhrase);
    }


}



