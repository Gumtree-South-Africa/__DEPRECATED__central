package com.ecg.mde.filter.comaservice;

import com.ecg.mde.filter.comaservice.filters.ComaFilterService;
import com.ecg.mde.filter.comaservice.filters.ContactMessage;
import com.ecg.mde.filter.comaservice.filters.ContactMessageAssembler;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;

import static com.ecg.mde.filter.comaservice.FilterService.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class FilterServiceTest {
    @Mock
    private ComaFilterService mockComaFilterService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MessageProcessingContext processingContext;
    @Mock
    private ContactMessageAssembler mockContactMessageAssembler;
    private FilterService filterService;
    @Mock
    private ContactMessage contactMessage;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        filterService = new FilterService(mockComaFilterService, mockContactMessageAssembler);
    }

    @Test
    public void doesNotFilterAnyMessageSentByDealer() {
        headerSentByDealer();
        filterService.filter(processingContext);

        headerSentByDealer();
        headerViaConversation();
        filterService.filter(processingContext);

        verifyZeroInteractions(mockComaFilterService);
    }

    @Test
    public void filtersContactMessagesNotSentByDealer() {
        headerSentByFsbo();
        when(mockContactMessageAssembler.getContactMessage(processingContext)).thenReturn(contactMessage);

        filterService.filter(processingContext);

        verify(mockComaFilterService, atLeastOnce()).getFilterResultsForMessage(contactMessage);
        verify(mockComaFilterService, never()).getFilterResultsForConversation(contactMessage);
    }

    @Test
    public void filtersConversationMessagesNotSentByDealer() {
        headerSentByFsbo();
        headerViaConversation();
        when(mockContactMessageAssembler.getContactMessage(processingContext)).thenReturn(contactMessage);

        filterService.filter(processingContext);

        verify(mockComaFilterService, atLeastOnce()).getFilterResultsForConversation(contactMessage);
        verify(mockComaFilterService, never()).getFilterResultsForMessage(contactMessage);
    }

    private void headerSentByFsbo() {
        when(processingContext.getMessage().getHeaders().get(CUSTOM_HEADER_BUYER_TYPE))
                .thenReturn(FilterService.DEALER + "somethingElse");
    }

    private void headerViaConversation() {
        when(processingContext.getMessage().getHeaders().get(CUSTOM_HEADER_MESSAGE_TYPE))
                .thenReturn(MESSAGE_TYPE_CONVERSATION);
    }

    private void headerSentByDealer() {
        when(processingContext.getMessage().getHeaders().get(CUSTOM_HEADER_BUYER_TYPE))
                .thenReturn(FilterService.DEALER);
    }


}
