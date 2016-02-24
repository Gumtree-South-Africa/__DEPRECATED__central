package com.ecg.messagecenter.pushmessage;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class AdImageLookupTest {

    private static final String AD_WITH_IMAGES = "{\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\":{\"declaredType\":\"com.ebay.ecg.api.spec.v1.schema.ad.Ad\",\"globalScope\":true,\"typeSubstituted\":true,\"nil\":false,\"scope\":\"javax.xml.bind.JAXBElement$GlobalScope\",\"name\":\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\",\"value\":{\"title\":{\"value\":\"Rc Scaler und&#x2F;oder Crawler fahrer gesucht nähe  Meiningen\"},\"pictures\":{\"picture\":[{\"link\":[{\"href\":\"http://i.ebayimg.com/00/s/NjQwWDEwMjQ=/z/ZIAAAOxy4dNS8gay/$_14.JPG\",\"rel\":\"thumbnail\"},{\"href\":\"http://i.ebayimg.com/00/s/NjQwWDEwMjQ=/z/ZIAAAOxy4dNS8gay/$_74.JPG\",\"rel\":\"teaser\"},{\"href\":\"http://i.ebayimg.com/00/s/NjQwWDEwMjQ=/z/ZIAAAOxy4dNS8gay/$_75.JPG\",\"rel\":\"large\"},{\"href\":\"http://i.ebayimg.com/00/s/NjQwWDEwMjQ=/z/ZIAAAOxy4dNS8gay/$_20.JPG\",\"rel\":\"extraLarge\"},{\"href\":\"http://i.ebayimg.com/00/s/NjQwWDEwMjQ=/z/ZIAAAOxy4dNS8gay/$_{imageId}.JPG\",\"rel\":\"canonicalUrl\"}]}]},\"ad-status\":{\"value\":\"ACTIVE\"},\"id\":\"178485801\",\"version\":\"1.16\",\"locale\":\"de_DE\",\"otherAttributes\":{}}}}";
    private static final String AD_WITHOUT_IMAGES= "{\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\":{\"name\":\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\",\"value\":{\"title\":{\"value\":\"2,40 Meter hoher Katzen Kratzbaum\"},\"pictures\":{},\"ad-status\":{\"value\":\"ACTIVE\"},\"id\":\"178487141\",\"version\":\"1.16\",\"locale\":\"de_DE\",\"otherAttributes\":{}},\"globalScope\":true,\"typeSubstituted\":true,\"declaredType\":\"com.ebay.ecg.api.spec.v1.schema.ad.Ad\",\"scope\":\"javax.xml.bind.JAXBElement$GlobalScope\",\"nil\":false}}";

    private HttpResponse httpResponse;

    @Before
    public void setUp() throws Exception {
        httpResponse = mock(HttpResponse.class,RETURNS_DEEP_STUBS);
    }

    @Test
    public void adWithImages() throws Exception {
        httpReturnsPayload(AD_WITH_IMAGES);

        String url = new AdImageLookup.AdImageUrlResponseHandler().handleResponse(httpResponse);

        assertEquals("http://i.ebayimg.com/00/s/NjQwWDEwMjQ=/z/ZIAAAOxy4dNS8gay/$_{imageId}.JPG", url);
    }


    @Test
    public void adWithoutImages() throws Exception {
        httpReturnsPayload(AD_WITHOUT_IMAGES);

        String url = new AdImageLookup.AdImageUrlResponseHandler().handleResponse(httpResponse);

        assertEquals("", url);
    }



    private void httpReturnsPayload(String adWithImages) throws IOException {
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(adWithImages.getBytes("UTF-8")));
    }
}
