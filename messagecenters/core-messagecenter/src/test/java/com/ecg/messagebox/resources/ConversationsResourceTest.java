package com.ecg.messagebox.resources;

import com.ecg.messagebox.model.PostBox;
import com.ecg.messagebox.model.Visibility;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConversationsResource.class)
public class ConversationsResourceTest extends AbstractTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostBoxService postBoxService;

    @Test
    public void getEmptyPostbox() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Collections.emptyList(), unreadCounts, 0);

        when(postBoxService.getConversations(USER_BUYER_ID, Visibility.ACTIVE, 0, 50))
                .thenReturn(postBox);

        mvc.perform(get("/users/" + USER_BUYER_ID + "/conversations/")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("offset", is(0)))
                .andExpect(jsonPath("limit", is(50)))
                .andExpect(jsonPath("totalCount", is(0)))
                .andExpect(jsonPath("$.conversations", hasSize(0)))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("conversationsWithUnreadMessagesCount", is(0)));
    }

    @Test
    public void getOneConversationPostbox() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Collections.singletonList(conversationThread()), unreadCounts, 1);

        when(postBoxService.getConversations(USER_BUYER_ID, Visibility.ACTIVE, 0, 50))
                .thenReturn(postBox);

        mvc.perform(get("/users/" + USER_BUYER_ID + "/conversations/")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("offset", is(0)))
                .andExpect(jsonPath("limit", is(50)))
                .andExpect(jsonPath("totalCount", is(1)))
                .andExpect(jsonPath("$.conversations", hasSize(1)))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("conversationsWithUnreadMessagesCount", is(0)));
    }

    @Test
    public void getOneArchivedConversationPostbox() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Collections.singletonList(conversationThread()), unreadCounts, 1);

        when(postBoxService.getConversations(USER_BUYER_ID, Visibility.ARCHIVED, 0, 50))
                .thenReturn(postBox);

        mvc.perform(get("/users/" + USER_BUYER_ID + "/conversations/")
                .contentType(MediaType.APPLICATION_JSON)
                .param("visibility", "archived"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("offset", is(0)))
                .andExpect(jsonPath("limit", is(50)))
                .andExpect(jsonPath("totalCount", is(1)))
                .andExpect(jsonPath("$.conversations", hasSize(1)))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("conversationsWithUnreadMessagesCount", is(0)));
    }

    @Test
    public void getMultiConversationsPostbox() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Arrays.asList(conversationThread(), conversationThread()), unreadCounts, 2);

        when(postBoxService.getConversations(USER_BUYER_ID, Visibility.ACTIVE, 0, 50))
                .thenReturn(postBox);

        mvc.perform(get("/users/" + USER_BUYER_ID + "/conversations/")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("offset", is(0)))
                .andExpect(jsonPath("limit", is(50)))
                .andExpect(jsonPath("totalCount", is(2)))
                .andExpect(jsonPath("$.conversations", hasSize(2)))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("conversationsWithUnreadMessagesCount", is(0)));
    }

    @Test
    public void archiveConversation() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Arrays.asList(conversationThread(), conversationThread()), unreadCounts, 2);

        when(postBoxService.archiveConversations(USER_BUYER_ID, Collections.singletonList(CONVERSATION_ID), 0, 50))
                .thenReturn(postBox);

        mvc.perform(put("/users/" + USER_BUYER_ID + "/conversations/archive")
                .param("ids", CONVERSATION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("offset", is(0)))
                .andExpect(jsonPath("limit", is(50)))
                .andExpect(jsonPath("totalCount", is(2)))
                .andExpect(jsonPath("$.conversations", hasSize(2)))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("conversationsWithUnreadMessagesCount", is(0)));
    }

    @Test
    public void multiArchiveConversation() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Arrays.asList(conversationThread(), conversationThread()), unreadCounts, 2);

        when(postBoxService.archiveConversations(USER_BUYER_ID, Arrays.asList("conversation-1", "conversation-2"), 0, 50))
                .thenReturn(postBox);

        mvc.perform(put("/users/" + USER_BUYER_ID + "/conversations/archive")
                .param("ids", "conversation-1", "conversation-2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void commaSeparatedArchiveConversation() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Arrays.asList(conversationThread(), conversationThread()), unreadCounts, 2);

        when(postBoxService.archiveConversations(USER_BUYER_ID, Arrays.asList("conversation-1", "conversation-2"), 0, 50))
                .thenReturn(postBox);

        mvc.perform(put("/users/" + USER_BUYER_ID + "/conversations/archive")
                .param("ids", "conversation-1,conversation-2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void activateConversation() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Arrays.asList(conversationThread(), conversationThread()), unreadCounts, 2);

        when(postBoxService.activateConversations(USER_BUYER_ID, Collections.singletonList(CONVERSATION_ID), 0, 50))
                .thenReturn(postBox);

        mvc.perform(put("/users/" + USER_BUYER_ID + "/conversations/activate")
                .param("ids", CONVERSATION_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("userId", is(USER_BUYER_ID)))
                .andExpect(jsonPath("offset", is(0)))
                .andExpect(jsonPath("limit", is(50)))
                .andExpect(jsonPath("totalCount", is(2)))
                .andExpect(jsonPath("$.conversations", hasSize(2)))
                .andExpect(jsonPath("unreadMessagesCount", is(0)))
                .andExpect(jsonPath("conversationsWithUnreadMessagesCount", is(0)));
    }

    @Test
    public void multiActivateConversation() throws Exception {
        UserUnreadCounts unreadCounts = new UserUnreadCounts(USER_BUYER_ID, 0, 0);
        PostBox postBox = new PostBox(USER_BUYER_ID, Arrays.asList(conversationThread(), conversationThread()), unreadCounts, 2);

        when(postBoxService.activateConversations(USER_BUYER_ID, Arrays.asList("conversation-1", "conversation-2"), 0, 50))
                .thenReturn(postBox);

        mvc.perform(put("/users/" + USER_BUYER_ID + "/conversations/activate")
                .param("ids", "conversation-1", "conversation-2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getSingleConversationByID() throws Exception {
        when(postBoxService.getConversationsById(USER_ID, AD_ID, 500))
                .thenReturn(Collections.singletonList(CONVERSATION_ID));

        mvc.perform(get("/users/" + USER_ID + "/ads/" + AD_ID + "/conversations/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is(CONVERSATION_ID)));
    }

    @Test
    public void getMultiConversationByID() throws Exception {
        when(postBoxService.getConversationsById(USER_ID, AD_ID, 500))
                .thenReturn(Arrays.asList("conversation-1", "conversation-2"));

        mvc.perform(get("/users/" + USER_ID + "/ads/" + AD_ID + "/conversations/ids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", is("conversation-1")))
                .andExpect(jsonPath("$[1]", is("conversation-2")));
    }

    @Test
    public void getEmptyConversationByID() throws Exception {
        when(postBoxService.getConversationsById(USER_ID, AD_ID, 500))
                .thenReturn(Collections.emptyList());

        mvc.perform(get("/users/" + USER_ID + "/ads/" + AD_ID + "/conversations/ids")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
