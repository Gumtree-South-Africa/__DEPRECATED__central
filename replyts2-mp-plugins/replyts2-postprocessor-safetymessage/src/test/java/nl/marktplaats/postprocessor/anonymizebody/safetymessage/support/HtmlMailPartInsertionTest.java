package nl.marktplaats.postprocessor.anonymizebody.safetymessage.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by reweber on 19/10/15
 */
public class HtmlMailPartInsertionTest {

    @Test
    public void testInsertSafetyText() throws Exception {
        HtmlMailPartInsertion underTest = new HtmlMailPartInsertion();

        assertEquals("<html><p>safety test</p><br></html>", underTest.insertSafetyText("", "safety test"));

        assertEquals(
                "<html><p>safety test<br><br/><br /><br >" +
                        "more text <a href=\"http://test.com\">a link</a> " +
                        "<a href=\"mailto:test@test.com\">a mail link</a></p><br></html><html><p>Hi!</p></html>",
                underTest.insertSafetyText(
                        "<html><p>Hi!</p></html>",
                        "safety test<br><br/><br /><br >" +
                                "more text <a href=\"http://test.com\">a link</a> " +
                                "<a href=\"mailto:test@test.com\">a mail link</a>"));
    }
}