package nl.marktplaats.postprocessor.anonymizebody.safetymessage.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by reweber on 19/10/15
 */
public class PlainTextMailPartInsertionTest {
    @Test
    public void testInsertSafetyText() throws Exception {
        PlainTextMailPartInsertion underTest = new PlainTextMailPartInsertion();
        assertEquals("safety test\r\n\r\n", underTest.insertSafetyText("", "safety test"));
        assertEquals(
                "safety test\r\n\r\n\r\n\r\nmore text http://test.com test@test.com\r\n\r\nHi!",
                underTest.insertSafetyText(
                        "Hi!",
                        "safety test<br><br/><br /><br >more text " +
                                "<a href=\"http://test.com\">a link</a> " +
                                "<a href=\"mailto:test@test.com\">a mail link</a>"));
    }

    @Test
    public void testCleanHtml() throws Exception {
        PlainTextMailPartInsertion underTest = new PlainTextMailPartInsertion();
        assertEquals(
                "Voor uw bescherming zijn e-mailadresssen nu afgeschermd. Uw reactie wordt via Marktplaats " +
                        "verzonden. http://marktplaats.custhelp.com/cgi-bin/marktplaats.cfg/php/enduser/std_alp.php " +
                        "Als u deze e-mail als ongewenst wilt melden, of als u deze e-mail niet vertrouwt, stuur dit " +
                        "bericht dan door naar spam@marktplaats.nl \r\n\r\nHi!",
                underTest.insertSafetyText(
                        "Hi!",
                        "<STYLE>*{padding:0;margin:0;}HTML{height:100%;width:100%;}body{width:100%;height:100%;margin:0;padding:0;margin-left:10px;margin-right:10px;}img{display:block;}</STYLE><table cellpadding=0 cellspacing=0 width=\"100%\" border=0><tr align=left valign=top><td><table cellpadding=0 cellspacing=4 width=\"100%\"><tr><td><font face=\"Arial, Helvetica, sans-serif\" size=2 color=\"#333333\" style=\"font-size:13px; line-height:17px\">Voor uw bescherming zijn e-mailadresssen nu afgeschermd. Uw reactie wordt via Marktplaats verzonden. <a href=\"http://marktplaats.custhelp.com/cgi-bin/marktplaats.cfg/php/enduser/std_alp.php\">Lees meer</a> Als u deze e-mail als ongewenst wilt melden, of als u deze e-mail niet vertrouwt, stuur dit bericht dan door naar <a href=\"mailto:spam@marktplaats.nl\">spam@marktplaats.nl</a></font></td></table></td><tr><td align=left valign=top bgcolor=\"#EEB47C\"><table cellpadding=0 cellspacing=0 width=600 border=0><tr><td>&nbsp;</td><td width=600 height=49 align=left bgcolor=\"#EEB47C\"><img style=display:block src=\"http://images.emessaging.nl/RS/MAR11-XXX_test/service/beeld_logo.gif\" alt=Marktplaats width=216 height=49 border=0></td></table></td><tr><td height=1 bgcolor=\"#c8884b\"></td><tr><td height=8 bgcolor=\"#e1e8f5\"></td><tr><td height=1 bgcolor=\"#b1c4df\"></td></table>"));
    }
}