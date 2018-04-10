package com.ecg.mde.filter.badword;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import de.mobile.cs.filter.domain.BadwordDTO;
import de.mobile.cs.filter.domain.BadwordType;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.ecg.replyts.core.api.model.conversation.FilterResultState.HELD;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BadwordFilterTest {

    private BadwordFilter instance;

    private CsFilterServiceClient filterServiceClient;

    private MessageProcessingContext context;

    private Message message;

    @Before
    public void before() {
        context = mock(MessageProcessingContext.class);
        message = mock(Message.class);
        filterServiceClient = mock(CsFilterServiceClient.class);
        instance = new BadwordFilter(filterServiceClient);
        when(context.getMessage()).thenReturn(message);
    }

    @Test
    public void filterServiceTest() {
        when(message.getPlainTextBody()).thenReturn("simple swearword text");
        when(filterServiceClient.filterSwearwords("simple swearword text")).thenReturn(
                Collections.singletonList(
                        BadwordDTO.newBuilder()
                                .term("swearword")
                                .stemmed("swearword")
                                .type(BadwordType.SWEARWORD)
                                .build()));

        List<FilterFeedback> list = instance.filter(context);
        assertThat(list, hasSize(1));

        FilterFeedback feedback = list.get(0);

        assertThat(feedback.getDescription(), is("text contains swearwords swearword"));
        assertThat(feedback.getUiHint(), is("BadwordFilter"));
        assertThat(feedback.getResultState(), is(HELD));
    }


    @Test
    public void filterServiceWithMoreThan10SwearwordsTest() {
        when(message.getPlainTextBody()).thenReturn("simple swearword text");

        final List<BadwordDTO> badwords = IntStream.range(0, 11).boxed()
                .map(i -> BadwordDTO.newBuilder()
                        .term("swearword-" + i)
                        .stemmed("swearword-" + i)
                        .type(BadwordType.SWEARWORD)
                        .build())
                .collect(Collectors.toList());

        when(filterServiceClient.filterSwearwords("simple swearword text")).thenReturn(badwords);

        List<FilterFeedback> list = instance.filter(context);
        assertThat(list, hasSize(1));

        FilterFeedback feedback = list.get(0);

        assertThat(feedback.getDescription(), is("text contains swearwords swearword-0 swearword-1 swearword-2 swearword-3 swearword-4 swearword-5 swearword-6 swearword-7 swearword-8 swearword-9..."));
        assertThat(feedback.getUiHint(), is("BadwordFilter"));
        assertThat(feedback.getResultState(), is(HELD));

    }

    @Test
    public void filterServiceEmptyBadwordListTest() {
        when(message.getPlainTextBody()).thenReturn("simple text");
        when(filterServiceClient.filterSwearwords("simple text")).thenReturn(new LinkedList<>());

        List<FilterFeedback> list = instance.filter(context);

        assertThat(list, hasSize(0));
    }
}