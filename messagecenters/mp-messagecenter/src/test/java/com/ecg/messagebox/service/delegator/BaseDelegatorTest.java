package com.ecg.messagebox.service.delegator;

import com.ecg.messagebox.converters.ConversationResponseConverter;
import com.ecg.messagebox.converters.PostBoxResponseConverter;
import com.ecg.messagebox.converters.UnreadCountsConverter;
import com.ecg.messagebox.diff.Diff;
import com.ecg.messagebox.configuration.DiffConfiguration;
import com.ecg.messagebox.configuration.NewModelConfiguration;
import com.ecg.messagebox.service.PostBoxService;
import com.ecg.messagecenter.persistence.PostBoxUnreadCounts;
import com.ecg.messagecenter.webapi.responses.ConversationResponse;
import com.ecg.messagecenter.webapi.responses.PostBoxResponse;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.Message;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class BaseDelegatorTest {

    // mocked old and new postBox services
    com.ecg.messagecenter.persistence.PostBoxService oldPbService = mock(com.ecg.messagecenter.persistence.PostBoxService.class);
    PostBoxService newPbService = mock(PostBoxService.class);

    // mocked converters
    PostBoxResponseConverter pbRespConverter = mock(PostBoxResponseConverter.class);
    UnreadCountsConverter unreadCountsConverter = mock(UnreadCountsConverter.class);
    ConversationResponseConverter convRespConverter = mock(ConversationResponseConverter.class);

    // mocked diff tool
    Diff diff = mock(Diff.class);

    NewModelConfiguration newModelConfig = mock(NewModelConfiguration.class);
    DiffConfiguration diffConfig = mock(DiffConfiguration.class);

    Conversation rtsConversation = mock(Conversation.class);
    Message rtsMessage = mock(Message.class);

    ConversationResponse convResponse = mock(ConversationResponse.class);
    PostBoxResponse pbResponse = mock(PostBoxResponse.class);
    PostBoxUnreadCounts oldPbUnreadCounts = mock(PostBoxUnreadCounts.class);

    static final int CORE_POOL_SIZE = 0;
    static final int MAX_POOL_SIZE = 1;
    static final int DIFF_POOL_SIZE = 1;

    static final int MESSAGES_LIMIT = 500;
    static final String USER_ID = "123";
    static final String CONV_ID = "c1";
    static final List<String> CONV_IDS = Arrays.asList("c1", "c2");
    static final ConversationRole CONV_ROLE = ConversationRole.Buyer;
    static final boolean NEW_REPLY_ARRIVED = true;

    static final int PAGE = 0;
    static final int SIZE = 100;

    static final String EXPECTED_ERROR_MSG_FROM_OLD = "~ expected from old model ~";
    static final String EXPECTED_ERROR_MSG_FROM_NEW = "~ expected from new model ~";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    <T> T verifyWithTimeout(T mock) {
        return verify(mock, timeout(100));
    }
}