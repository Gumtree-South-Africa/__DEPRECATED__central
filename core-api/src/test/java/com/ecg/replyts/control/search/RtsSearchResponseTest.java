package com.ecg.replyts.control.search;

import com.ecg.replyts.core.api.search.RtsSearchResponse;
import org.junit.Test;

import java.util.Collections;

import static com.ecg.replyts.core.api.search.RtsSearchResponse.NOT_APPLICABLE;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class RtsSearchResponseTest {
    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void invalidOffsetTriggersException() {
        new RtsSearchResponse(Collections.EMPTY_LIST, 1, NOT_APPLICABLE, NOT_APPLICABLE);
        fail("Should have failed in call to constructor above!");
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void invalidCountTriggersException() {
        new RtsSearchResponse(Collections.EMPTY_LIST, NOT_APPLICABLE, 1, NOT_APPLICABLE);
        fail("Should have failed in call to constructor above!");
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressWarnings("unchecked")
    public void invalidTotalTriggersException() {
        new RtsSearchResponse(Collections.EMPTY_LIST, NOT_APPLICABLE, NOT_APPLICABLE, 1);
        fail("Should have failed in call to constructor above!");
    }

    @Test
    public void validPaginationSettingsSucceed() {
        //these must work
        new RtsSearchResponse(Collections.EMPTY_LIST);
        new RtsSearchResponse(Collections.EMPTY_LIST, 0, 1, 2);
    }

    @Test
    public void partialResponseCorrectlyIndicated() {
        RtsSearchResponse rtsSearchResponse1 = new RtsSearchResponse(Collections.EMPTY_LIST);
        RtsSearchResponse rtsSearchResponse2 = new RtsSearchResponse(Collections.EMPTY_LIST, 0, 1, 2);

        assertThat(rtsSearchResponse1.isPartialResult(), is(false));
        assertThat(rtsSearchResponse2.isPartialResult(), is(true));
    }
}