package com.ecg.de.mobile.replyts.comafilterservice.filters;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.mock;

public class ComaFilterServiceTest {
    
    @Test
    public void testDoNotFilterWhenDeactivated() throws Exception {

        ComaFilterServiceClient comaFilterServiceClient = mock(ComaFilterServiceClient.class);
        ComaFilterService comaFilterService = new ComaFilterService(comaFilterServiceClient,false);

        Collection<String> result = comaFilterService.getFilterResults(mock(ContactMessage.class));
        Assert.assertEquals(result.size(), 0);
    }


    @Test
    public void testServiceRespondsToValidRequests() throws Exception {

        ComaFilterServiceClient comaFilterServiceClient = Mockito.mock(ComaFilterServiceClient.class);
        ComaFilterService comaFilterService = new ComaFilterService(comaFilterServiceClient,true);

        List<String> expectedResult = Lists.newArrayList("foo filter", "bar filter");

        ContactMessage contactMessage = new ContactMessage();

        Mockito.when(comaFilterServiceClient.getFilterResults(contactMessage)).thenReturn(expectedResult);

        Collection<String> actualResult = comaFilterService.getFilterResults(contactMessage);
        Assert.assertEquals(expectedResult, actualResult);

    }
}