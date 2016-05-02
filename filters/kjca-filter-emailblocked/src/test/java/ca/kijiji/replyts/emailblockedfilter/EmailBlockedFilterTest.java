package ca.kijiji.replyts.emailblockedfilter;

import ca.kijiji.replyts.LeGridClient;
import com.ecg.replyts.core.api.model.conversation.ConversationState;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.ecg.replyts.core.api.model.conversation.ModerationResultState;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeGuard;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableConversation;
import com.ecg.replyts.core.runtime.model.conversation.ImmutableMessage;
import com.ecg.replyts.core.runtime.persistence.conversation.DefaultMutableConversation;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import mockit.Deencapsulation;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static ca.kijiji.replyts.emailblockedfilter.EmailBlockedFilter.IS_BLOCKED_KEY;
import static com.ecg.replyts.core.api.model.conversation.FilterResultState.DROPPED;
import static com.ecg.replyts.core.api.model.conversation.FilterResultState.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JMockit.class)
public class EmailBlockedFilterTest {

    private static final String BUYER_EMAIL = "buyer@kijiji.ca";
    private static final String SELLER_EMAIL = "seller@kijiji.ca";
    private static final int SCORE = 100;

    @Tested
    private EmailBlockedFilter emailBlockedFilter;

    @Injectable
    private LeGridClient leGridClient;

    @Mocked
    private Mail mail;

    private MessageProcessingContext mpc;
    private ImmutableConversation.Builder conversationBuilder;
    private ImmutableMessage.Builder messageBuilder;

    @Before
    public void setUp() throws Exception {
        emailBlockedFilter = new EmailBlockedFilter(SCORE, leGridClient);

        messageBuilder = ImmutableMessage.Builder
                .aMessage()
                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                .withState(MessageState.UNDECIDED)
                .withReceivedAt(DateTime.now())
                .withLastModifiedAt(DateTime.now())
                .withFilterResultState(OK)
                .withHumanResultState(ModerationResultState.UNCHECKED)
                .withHeaders(ImmutableMap.<String, String>of())
                .withPlainTextBody("")
                .withProcessingFeedback(ImmutableList.<ProcessingFeedback>of())
                .withLastEditor(Optional.<String>absent());

        conversationBuilder = ImmutableConversation.Builder
                .aConversation()
                .withId("cid")
                .withState(ConversationState.ACTIVE)
                .withBuyer(BUYER_EMAIL, "b.secret@rts.kijiji.ca")
                .withSeller(SELLER_EMAIL, "s.secret@rts.kijiji.ca")
                .withMessages(
                        ImmutableList.of(messageBuilder.build()));

        mpc = new MessageProcessingContext(mail, "msgId", new ProcessingTimeGuard(0));
    }

    @Test
    public void buyer_emailBlocked() throws Exception {
        mpc.setConversation(Deencapsulation.newInstance(DefaultMutableConversation.class, conversationBuilder.build()));
        mpc.setMessageDirection(MessageDirection.BUYER_TO_SELLER);

        new Expectations() {{
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", BUYER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.TRUE);
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", SELLER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.FALSE);
        }};

        List<FilterFeedback> feedbacks = emailBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(1));
        FilterFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getUiHint(), is("buyer email is blocked"));
        assertThat(feedback.getDescription(), is("Buyer email is blocked"));
        assertThat(feedback.getResultState(), is(DROPPED));
        assertThat(feedback.getScore(), is(SCORE));
    }

    @Test
    public void nothingBlocked() throws Exception {
        mpc.setConversation(Deencapsulation.newInstance(DefaultMutableConversation.class, conversationBuilder.build()));
        mpc.setMessageDirection(MessageDirection.BUYER_TO_SELLER);

        new Expectations() {{
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", BUYER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.FALSE);
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", SELLER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.FALSE);
        }};

        List<FilterFeedback> feedbacks = emailBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(0));
    }

    @Test
    public void bothBlocked() throws Exception {
        mpc.setConversation(Deencapsulation.newInstance(DefaultMutableConversation.class, conversationBuilder.build()));
        mpc.setMessageDirection(MessageDirection.BUYER_TO_SELLER);

        new Expectations() {{
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", BUYER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.TRUE);
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", SELLER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.TRUE);
        }};

        List<FilterFeedback> feedbacks = emailBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(2));
        FilterFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getUiHint(), is("buyer email is blocked"));
        assertThat(feedback.getDescription(), is("Buyer email is blocked"));
        assertThat(feedback.getResultState(), is(DROPPED));
        assertThat(feedback.getScore(), is(SCORE));
        feedback = feedbacks.get(1);
        assertThat(feedback.getUiHint(), is("seller email is blocked"));
        assertThat(feedback.getDescription(), is("Seller email is blocked"));
        assertThat(feedback.getResultState(), is(DROPPED));
        assertThat(feedback.getScore(), is(SCORE));
    }

    @Test
    public void seller_emailBlocked() throws Exception {
        conversationBuilder = conversationBuilder.withMessages(
                ImmutableList.of(
                        messageBuilder.build(),
                        messageBuilder
                                .withMessageDirection(MessageDirection.SELLER_TO_BUYER)
                                .build()
                )
        );
        mpc.setConversation(Deencapsulation.newInstance(DefaultMutableConversation.class, conversationBuilder.build()));
        mpc.setMessageDirection(MessageDirection.SELLER_TO_BUYER);

        new Expectations() {{
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", BUYER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.FALSE);
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", SELLER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.TRUE);
        }};

        List<FilterFeedback> feedbacks = emailBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(1));
        FilterFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getUiHint(), is("seller email is blocked"));
        assertThat(feedback.getDescription(), is("Seller email is blocked"));
        assertThat(feedback.getResultState(), is(DROPPED));
        assertThat(feedback.getScore(), is(SCORE));
    }

    @Test
    public void emailToSeller_sellerAddressBlocked() throws Exception {
        conversationBuilder = conversationBuilder.withMessages(
                ImmutableList.of(
                        messageBuilder.build(),
                        messageBuilder
                                .withMessageDirection(MessageDirection.BUYER_TO_SELLER)
                                .build()
                )
        );
        mpc.setConversation(Deencapsulation.newInstance(DefaultMutableConversation.class, conversationBuilder.build()));
        mpc.setMessageDirection(MessageDirection.SELLER_TO_BUYER);

        new Expectations() {{
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", BUYER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.FALSE);
            leGridClient.getJsonAsMap(String.format("replier/email/%s/is-blocked", SELLER_EMAIL));
            result = ImmutableMap.of(IS_BLOCKED_KEY, Boolean.TRUE);
        }};

        List<FilterFeedback> feedbacks = emailBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(1));
        FilterFeedback feedback = feedbacks.get(0);
        assertThat(feedback.getUiHint(), is("seller email is blocked"));
        assertThat(feedback.getDescription(), is("Seller email is blocked"));
        assertThat(feedback.getResultState(), is(DROPPED));
        assertThat(feedback.getScore(), is(SCORE));
    }

    @Test
    public void unknownDirection_emailNotBlocked() throws Exception {
        mpc.setConversation(Deencapsulation.newInstance(DefaultMutableConversation.class, conversationBuilder.build()));
        mpc.setMessageDirection(MessageDirection.UNKNOWN);

        List<FilterFeedback> feedbacks = emailBlockedFilter.filter(mpc);
        assertThat(feedbacks.size(), is(0));

        new FullVerifications(leGridClient) {};
    }
}
