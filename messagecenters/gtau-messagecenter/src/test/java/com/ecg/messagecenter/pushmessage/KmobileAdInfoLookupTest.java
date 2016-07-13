package com.ecg.messagecenter.pushmessage;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class KmobileAdInfoLookupTest {

    private static final String AD_WITH_IMAGES = "{\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\":{\"name\":\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\",\"value\":{\"price\":null,\"title\":{\"value\":\"Magic mouse\",\"size-min\":null,\"size-max\":null,\"since\":null,\"deprecated\":null,\"accessible\":null,\"read\":null,\"write\":null,\"search-response-included\":null,\"type\":null,\"sub-type\":null,\"localized-label\":null,\"search-param\":null,\"search-style\":null},\"description\":null,\"imprint\":null,\"email\":null,\"replyTemplate\":null,\"phone\":null,\"rank\":null,\"neighborhood\":null,\"link\":null,\"pictures\":{\"picture\":[{\"link\":[{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/mu8AAOSwBLlVQbzW/$_14.JPG\",\"rel\":\"thumbnail\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/mu8AAOSwBLlVQbzW/$_74.JPG\",\"rel\":\"normal\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/mu8AAOSwBLlVQbzW/$_75.JPG\",\"rel\":\"large\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/mu8AAOSwBLlVQbzW/$_20.JPG\",\"rel\":\"extraLarge\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/mu8AAOSwBLlVQbzW/$_57.JPG\",\"rel\":\"extraExtraLarge\",\"type\":null,\"hreflang\":null}],\"since\":null,\"deprecated\":null,\"accessible\":null,\"read\":null,\"write\":null,\"search-response-included\":null,\"type\":null,\"sub-type\":null,\"localized-label\":null,\"search-param\":null,\"search-style\":null},{\"link\":[{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/48EAAOSwymxVQbzj/$_14.JPG\",\"rel\":\"thumbnail\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/48EAAOSwymxVQbzj/$_74.JPG\",\"rel\":\"normal\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/48EAAOSwymxVQbzj/$_75.JPG\",\"rel\":\"large\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/48EAAOSwymxVQbzj/$_20.JPG\",\"rel\":\"extraLarge\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/48EAAOSwymxVQbzj/$_57.JPG\",\"rel\":\"extraExtraLarge\",\"type\":null,\"hreflang\":null}],\"since\":null,\"deprecated\":null,\"accessible\":null,\"read\":null,\"write\":null,\"search-response-included\":null,\"type\":null,\"sub-type\":null,\"localized-label\":null,\"search-param\":null,\"search-style\":null}]},\"bids\":null,\"id\":\"1396\",\"version\":\"1.18\",\"locale\":\"en_AU\",\"supported-locales\":null,\"otherAttributes\":{}},\"scope\":\"javax.xml.bind.JAXBElement$GlobalScope\",\"declaredType\":\"com.ebay.ecg.api.spec.v1.schema.ad.Ad\",\"globalScope\":true,\"nil\":false,\"typeSubstituted\":true}}";
    private static final String AD_WITH_ONE_IMAGE = "{\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\":{\"name\":\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\",\"value\":{\"price\":null,\"title\":{\"value\":\"Sexy laptop\",\"size-min\":null,\"size-max\":null,\"since\":null,\"deprecated\":null,\"accessible\":null,\"read\":null,\"write\":null,\"search-response-included\":null,\"type\":null,\"sub-type\":null,\"localized-label\":null,\"search-param\":null,\"search-style\":null},\"description\":null,\"imprint\":null,\"email\":null,\"replyTemplate\":null,\"phone\":null,\"rank\":null,\"neighborhood\":null,\"link\":null,\"pictures\":{\"picture\":[{\"link\":[{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/ZPEAAOSwcwhVQbsj/$_14.JPG\",\"rel\":\"thumbnail\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/ZPEAAOSwcwhVQbsj/$_74.JPG\",\"rel\":\"normal\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/ZPEAAOSwcwhVQbsj/$_75.JPG\",\"rel\":\"large\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/ZPEAAOSwcwhVQbsj/$_20.JPG\",\"rel\":\"extraLarge\",\"type\":null,\"hreflang\":null},{\"href\":\"http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/ZPEAAOSwcwhVQbsj/$_57.JPG\",\"rel\":\"extraExtraLarge\",\"type\":null,\"hreflang\":null}],\"since\":null,\"deprecated\":null,\"accessible\":null,\"read\":null,\"write\":null,\"search-response-included\":null,\"type\":null,\"sub-type\":null,\"localized-label\":null,\"search-param\":null,\"search-style\":null}]},\"bids\":null,\"id\":\"1395\",\"version\":\"1.18\",\"locale\":\"en_AU\",\"supported-locales\":null,\"otherAttributes\":{}},\"scope\":\"javax.xml.bind.JAXBElement$GlobalScope\",\"declaredType\":\"com.ebay.ecg.api.spec.v1.schema.ad.Ad\",\"globalScope\":true,\"nil\":false,\"typeSubstituted\":true}}";
    private static final String AD_WITHOUT_IMAGES= "{\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\":{\"name\":\"{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ad\",\"value\":{\"title\":{\"value\":\"2,40 Meter hoher Katzen Kratzbaum\"},\"pictures\":{\"picture\":null},\"ad-status\":{\"value\":\"ACTIVE\"},\"id\":\"178487141\",\"version\":\"1.16\",\"locale\":\"de_DE\",\"otherAttributes\":{}},\"globalScope\":true,\"typeSubstituted\":true,\"declaredType\":\"com.ebay.ecg.api.spec.v1.schema.ad.Ad\",\"scope\":\"javax.xml.bind.JAXBElement$GlobalScope\",\"nil\":false}}";

    private HttpResponse httpResponse;

    @Before
    public void setUp() throws Exception {
        httpResponse = mock(HttpResponse.class,RETURNS_DEEP_STUBS);
    }

    @Test
    public void adWithImages() throws Exception {
        httpReturnsPayload(AD_WITH_IMAGES);

        Optional<AdInfoLookup.AdInfo> adInfo = new AdInfoLookup.AdInfoResponseHandler().handleResponse(httpResponse);

        assertEquals("http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/mu8AAOSwBLlVQbzW/$_75.JPG", adInfo.get().getImageUrl());
        assertEquals("Magic mouse", adInfo.get().getTitle());
    }

    @Test
    public void adWithOneImage() throws Exception {
        httpReturnsPayload(AD_WITH_ONE_IMAGE);

        Optional<AdInfoLookup.AdInfo> adInfo = new AdInfoLookup.AdInfoResponseHandler().handleResponse(httpResponse);

        assertEquals("http://i.ebayimg.com/00/s/MTYwMFgxMjAw/z/ZPEAAOSwcwhVQbsj/$_75.JPG", adInfo.get().getImageUrl());
        assertEquals("Sexy laptop", adInfo.get().getTitle());
    }

    @Test
    public void adWithoutImages() throws Exception {
        httpReturnsPayload(AD_WITHOUT_IMAGES);

        Optional<AdInfoLookup.AdInfo> adInfo = new AdInfoLookup.AdInfoResponseHandler().handleResponse(httpResponse);

        assertEquals("", adInfo.get().getImageUrl());
        assertEquals("2,40 Meter hoher Katzen Kratzbaum", adInfo.get().getTitle());
    }



    private void httpReturnsPayload(String adWithImages) throws IOException {
        when(httpResponse.getStatusLine().getStatusCode()).thenReturn(200);
        when(httpResponse.getEntity().getContent()).thenReturn(new ByteArrayInputStream(adWithImages.getBytes("UTF-8")));
    }
}
