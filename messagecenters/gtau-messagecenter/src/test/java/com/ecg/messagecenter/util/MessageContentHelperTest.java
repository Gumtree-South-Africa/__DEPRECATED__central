package com.ecg.messagecenter.util;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.google.common.io.ByteStreams;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mdarapour on 13/05/15.
 */
public class MessageContentHelperTest {
    private static final String AUTOGATE_LEAD = "<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?><lead schemaVersion=\\\"1.0\\\" xmlns=\\\"http://dataconnect.carsales.com.au/schema/\\\"  xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\"  xsi:schemaLocation=\\\"http://dataconnect.carsales.com.au/schema/ https://testdataconnect.carsales.com.au/lead/lead.xml\\\"><sourceID>1054</sourceID><leadType>BNCIS</leadType><requestType>Dealer</requestType><sourceDealerID>54321</sourceDealerID><dealerName>Hugo&apos;s Half Price Cars</dealerName><comments>Hi</comments><prospect><firstName>Someone</firstName><email>isader@ebay.com</email><homePhone>0403333337</homePhone></prospect><itemInfo><stockNumber>W12955</stockNumber><price>19981.00</price><colour>White</colour><description>Gumtree sales lead for &quot;New Suzuki Jimny Sierra via BU&quot; - http://www.gumtree.dev/s-ad/australia/cars-vans-utes/new-suzuki-jimny-sierra-via-bu/1174</description><itemDetails><carInfo><makeName>Ford</makeName><modelName>Falcon</modelName><releaseYear>2014</releaseYear><bodyStyle>Ute</bodyStyle><transmission>Automatic</transmission><kilometres>9938</kilometres><rego>CJE13J</rego></carInfo></itemDetails></itemInfo></lead>";
    private static final String BUYER_NAME = "James via Gumtree";
    private static final String EMPTY_NAME = "via Gumtree";
    private static final String INVALID_NAME = " via Gumtree";

    @Test
    public void trueXml() {
        assertTrue(MessageContentHelper.isLooksLikeXml(AUTOGATE_LEAD));
    }

    @Test
    public void falseXml() {
        assertFalse(MessageContentHelper.isLooksLikeXml(null));
        assertFalse(MessageContentHelper.isLooksLikeXml(" "));
        assertFalse(MessageContentHelper.isLooksLikeXml(
                "Hi, welcome to Gumtree! <a href=\"http://www.qa6.gumtree.com.au\">link</a>"
        ));
    }

    @Test
    public void buyer_via_gumtree() {
        assertEquals("James", MessageContentHelper.senderName(BUYER_NAME));
    }

    @Test
    public void empty_name() {
        assertEquals("Anonymous", MessageContentHelper.senderName(EMPTY_NAME));
    }

    @Test
    public void invalid_name() {
        assertEquals("Anonymous", MessageContentHelper.senderName(INVALID_NAME));
    }

    @Test
    public void autogate_email() throws Exception {
        File email = new File(getClass().getResource("steve.eml").getFile());
        try (FileInputStream fin = new FileInputStream(email)) {
            Mail mail = Mails.readMail(ByteStreams.toByteArray(fin));
            List<String> parts = mail.getPlaintextParts();
            assertTrue("Message should be XML", MessageContentHelper.isLooksLikeXml(parts.get(0)));
        } catch (ParsingException | IOException e) {
            e.printStackTrace();
        }
    }
}
