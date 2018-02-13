package com.ecg.messagebox.resources;

import com.ecg.messagebox.service.PostBoxService;
import com.ecg.replyts.core.api.model.conversation.UserUnreadCounts;
import com.ecg.replyts.core.runtime.persistence.ObjectMapperConfigurer;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResponseDataResource.class)
public class UnreadCountsResourceTest extends AbstractTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PostBoxService postBoxService;

    @Test
    public void emptyUnreadCounts() throws Exception {
        when(postBoxService.getUnreadCounts(USER_ID))
                .thenReturn(new UserUnreadCounts(USER_ID, 0, 0));

        String response = ObjectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("userId", USER_ID)
                .put("conversationsWithUnreadMessagesCount", 0)
                .put("unreadMessagesCount", 0)
                .toString();

        mvc.perform(get("/users/" + USER_ID + "/unread-counts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json(response));
    }

    @Test
    public void nonEmptyUnreadCounts() throws Exception {
        when(postBoxService.getUnreadCounts(USER_ID))
                .thenReturn(new UserUnreadCounts(USER_ID, 2, 3));

        String response = ObjectMapperConfigurer.getObjectMapper()
                .createObjectNode()
                .put("userId", USER_ID)
                .put("conversationsWithUnreadMessagesCount", 2)
                .put("unreadMessagesCount", 3)
                .toString();

        mvc.perform(get("/users/" + USER_ID + "/unread-counts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().json(response));
    }
}
