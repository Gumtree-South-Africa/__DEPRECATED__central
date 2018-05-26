package com.ecg.comaas.gtuk.filter.category;

import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.model.conversation.command.ConversationCommand;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage.Builder.aMessage;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class GumtreeCategoryFilterTest {
    @InjectMocks
    private GumtreeCategoryFilter filter;

    @Mock
    private MessageProcessingContext messageProcessingContext;

    @Mock
    private CategoryService categoryService;

    @Captor
    private
    ArgumentCaptor<ConversationCommand> argumentCaptor;

    @Before
    public void setup() {
        ImmutableConversation conversation = newImmutableConversation();
        when(messageProcessingContext.getConversation()).thenReturn(conversation);
        when(categoryService.getHierarchy(eq(1234L))).thenReturn(Collections.emptyList());
    }

    @Test
    public void testCategory() {
        Map<String, Object> filterContext = new HashMap<>();
        when(messageProcessingContext.getFilterContext()).thenReturn(filterContext);
        when(categoryService.getFullPath(eq(1234L))).thenReturn(Arrays.asList(newCategory(1L, "a", 1), newCategory(2L, "b", 2)));

        filter.filter(messageProcessingContext);

        Set<Long> categoryBreadCrumb = (Set<Long>) filterContext.get("categoryBreadCrumb");
        assertThat(categoryBreadCrumb).isNotNull();
        assertThat(categoryBreadCrumb).containsExactly(1L, 2L);
    }

    @Test
    public void testAddCategoriesToConversation() throws Exception {
        List<Category> categoryHierarchy = Arrays.asList(newCategory(200L, "for sale", 1), newCategory(100L, "cars", 2));
        when(categoryService.getHierarchy(1234L)).thenReturn(categoryHierarchy);

        filter.filter(messageProcessingContext);

        verify(messageProcessingContext, times(2)).addCommand(argumentCaptor.capture());

        List<ConversationCommand> actual = argumentCaptor.getAllValues();
        AddCustomValueCommand c = (AddCustomValueCommand) actual.get(0);
        assertThat(c.getConversationId()).isEqualTo("2:vfbtp0:idr5s3l1");
        assertThat(c.getKey()).isEqualTo("l1-categoryid");
        assertThat(c.getValue()).isEqualTo("200");
        c = (AddCustomValueCommand) actual.get(1);
        assertThat(c.getConversationId()).isEqualTo("2:vfbtp0:idr5s3l1");
        assertThat(c.getKey()).isEqualTo("l2-categoryid");
        assertThat(c.getValue()).isEqualTo("100");
    }

    private Category newCategory(long id, String name, int depth) {
        Category category = new Category();
        category.setId(id);
        category.setDepth(depth);
        return category;
    }

    private ImmutableConversation newImmutableConversation() {
        Map<String, String> customValues = new HashMap<>();
        customValues.put("categoryid", "1234");

        return ImmutableConversation.Builder.aConversation()
                .withId("2:vfbtp0:idr5s3l1")
                .withCustomValues(customValues)
                .withCreatedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withState(ConversationState.ACTIVE)
                .withMessage(newMessage())
                .build();
    }

    private ImmutableMessage.Builder newMessage() {
        return aMessage()
                .withId("message1")
                .withEventTimeUUID(UUID.randomUUID())
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.SENT)
                .withReceivedAt(new DateTime())
                .withLastModifiedAt(new DateTime())
                .withHeader("X-Message-Type", "asq")
                .withTextParts(singletonList("text 123"))
                .withHeader("Subject", "subject");
    }
}
