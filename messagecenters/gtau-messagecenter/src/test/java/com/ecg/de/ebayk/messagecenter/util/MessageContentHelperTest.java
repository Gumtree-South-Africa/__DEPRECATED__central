package com.ecg.de.ebayk.messagecenter.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by mdarapour on 13/05/15.
 */
public class MessageContentHelperTest {
    public static final String AUTOGATE_LEAD = "<?xml version=\\\"1.0\\\" encoding=\\\"UTF-8\\\"?><lead schemaVersion=\\\"1.0\\\" xmlns=\\\"http://dataconnect.carsales.com.au/schema/\\\"  xmlns:xsi=\\\"http://www.w3.org/2001/XMLSchema-instance\\\"  xsi:schemaLocation=\\\"http://dataconnect.carsales.com.au/schema/ https://testdataconnect.carsales.com.au/lead/lead.xml\\\"><sourceID>1054</sourceID><leadType>BNCIS</leadType><requestType>Dealer</requestType><sourceDealerID>54321</sourceDealerID><dealerName>Hugo&apos;s Half Price Cars</dealerName><comments>Hi</comments><prospect><firstName>Someone</firstName><email>isader@ebay.com</email><homePhone>0403333337</homePhone></prospect><itemInfo><stockNumber>W12955</stockNumber><price>19981.00</price><colour>White</colour><description>Gumtree sales lead for &quot;New Suzuki Jimny Sierra via BU&quot; - http://www.gumtree.dev/s-ad/australia/cars-vans-utes/new-suzuki-jimny-sierra-via-bu/1174</description><itemDetails><carInfo><makeName>Ford</makeName><modelName>Falcon</modelName><releaseYear>2014</releaseYear><bodyStyle>Ute</bodyStyle><transmission>Automatic</transmission><kilometres>9938</kilometres><rego>CJE13J</rego></carInfo></itemDetails></itemInfo></lead>";
    public static final String BUYER_NAME = "James via Gumtree";
    public static final String EMPTY_NAME = "via Gumtree";
    public static final String INVALID_NAME = " via Gumtree";

    @Test
    public void trueXml() {
        assertTrue(MessageContentHelper.isXml(AUTOGATE_LEAD));
    }

    @Test
    public void falseXml() {
        assertFalse(MessageContentHelper.isXml(null));
        assertFalse(MessageContentHelper.isXml(" "));
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
}
