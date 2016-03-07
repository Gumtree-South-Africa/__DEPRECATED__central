package com.ecg.de.mobile.replyts.comafilterservice;

import com.ecg.de.mobile.replyts.comafilterservice.filters.ComaFilterService;
import com.ecg.de.mobile.replyts.comafilterservice.filters.ContactMessage;
import com.ecg.de.mobile.replyts.comafilterservice.filters.ContactMessageAssembler;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FilterServiceTest {

    @Test
    public void filtersContactMessagesLaterInConversationNotSentByDealer() {
        ComaFilterService mockComaFilterService = mock(ComaFilterService.class);
        ContactMessageAssembler mockContactMessageAssembler = mock(ContactMessageAssembler.class);
        FilterService fS = new FilterService(mockComaFilterService, mockContactMessageAssembler);

        MessageProcessingContext mock = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        when(mock.getMessage().getHeaders().containsKey(FilterService.CUSTOM_HEADER_PREFIX + "Seller_Type"))
                .thenReturn(true);
        when(mock.getMessage().getHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Buyer_Type"))
                .thenReturn(FilterService.DEALER + "somethingElse");
        when(mockContactMessageAssembler.getContactMessage(mock)).thenReturn(new ContactMessage());
        fS.filter(mock);
        verify(mockComaFilterService, atLeastOnce()).getFilterResults(any(ContactMessage.class));
    }



    @Test
    public void doesNotFilterContactMessagesSentByDealer() {
        ComaFilterService mockComaFilterService = mock(ComaFilterService.class);
        FilterService fS = new FilterService(mockComaFilterService);

        MessageProcessingContext mock = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        when(mock.getMessage().getHeaders().containsKey(FilterService.CUSTOM_HEADER_PREFIX + "Seller_Type"))
                .thenReturn(true);
        when(mock.getMessage().getHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Buyer_Type"))
                .thenReturn(FilterService.DEALER);
        fS.filter(mock);
        verify(mockComaFilterService, never()).getFilterResults(any(ContactMessage.class));
    }

    @Test
    public void doesNotFilterContactMessagesLaterInConversation() {
        ComaFilterService mockComaFilterService = mock(ComaFilterService.class);
        FilterService fS = new FilterService(mockComaFilterService);

        MessageProcessingContext mock = mock(MessageProcessingContext.class, RETURNS_DEEP_STUBS);
        when(mock.getMessage().getHeaders().containsKey(FilterService.CUSTOM_HEADER_PREFIX + "Seller_Type"))
                .thenReturn(false);
        when(mock.getMessage().getHeaders().get(FilterService.CUSTOM_HEADER_PREFIX + "Buyer_Type"))
                .thenReturn(FilterService.DEALER + "somethingElse");
        fS.filter(mock);
        verify(mockComaFilterService, never()).getFilterResults(any(ContactMessage.class));
    }


}
