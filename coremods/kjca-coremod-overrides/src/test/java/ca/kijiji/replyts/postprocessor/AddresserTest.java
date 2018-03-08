package ca.kijiji.replyts.postprocessor;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailcloaking.MultiTennantMailCloakingService;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AddresserTest {
    public static final String SELLER_DOMAIN_TLD = "seller@domain.tld";
    public static final String BUYER_DOMAIN_TLD = "buyer@domain.tld";
    protected static final String BUYER_CLOAKED = "buyer.cloaked@ebay.com";
    protected static final String SELLER_CLOAKED = "seller.cloaked@ebay.com";

    @Mock
    private MessageProcessingContext context;

    @Mock
    private MultiTennantMailCloakingService mailCloakingService;

    @Mock
    private MutableMail incomingMail;

    @Mock
    private MutableMail outgoingMail;

    @Mock
    private Conversation conversation;

    private Addresser addresser;

    @Before
    public void setUp() throws Exception {
        addresser = new Addresser(mailCloakingService);
        when(context.getConversation()).thenReturn(conversation);
        when(conversation.getUserIdFor(ConversationRole.Seller)).thenReturn(SELLER_DOMAIN_TLD);
        when(conversation.getUserIdFor(ConversationRole.Buyer)).thenReturn(BUYER_DOMAIN_TLD);
        Map<String, String> customValues = ImmutableMap.of("anonymize", "true");
        when(conversation.getCustomValues()).thenReturn(customValues);

        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress(BUYER_CLOAKED));
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Seller, conversation)).thenReturn(new MailAddress(SELLER_CLOAKED));
    }

    @Test
    public void englishLocale_b2s_fromNameMissing_headersSet_anonymized() throws Exception {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("en_CA");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn(null);
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(SELLER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("From", BUYER_CLOAKED);
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void frenchLocale_b2s_fromNameMissing_headersSet_anonymized() throws Exception {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("fr_CA");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn(null);
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(SELLER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("From", BUYER_CLOAKED);
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void fromNameSet_headersSet_anonymized() throws Exception {
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn("Mr. Smiley Face \uD83D\uDE00");
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(SELLER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("From", "=?UTF-8?Q?Mr=2E_Smiley_Face_=F0=9F=98=80?= <" + BUYER_CLOAKED + ">");
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test(expected = RuntimeException.class)
    public void invalidEmail_throwsException() throws Exception {
        when(mailCloakingService.createdCloakedMailAddress(ConversationRole.Buyer, conversation)).thenReturn(new MailAddress("something@weird@com"));
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("en_CA");
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);
    }

    @Test
    public void s2b_fromNameMissing_headersSet_anonymized() throws Exception {
        when(context.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(BUYER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("From", SELLER_CLOAKED);
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void s2b_fromNameSet_headersSet_anonymized() throws Exception {
        when(context.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getFromName()).thenReturn("Seller Name");
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(BUYER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("From", "Seller Name" + " <" + SELLER_CLOAKED + ">");
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void englishLocale_doNotAnonymize_fromNameMissing_headersSet() throws Exception {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("anonymize", "false"));
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("en_CA");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn(null);
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(SELLER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("Reply-To", BUYER_DOMAIN_TLD);
        verify(outgoingMail).addHeader("From", "\"Kijiji Reply (from buyer@domain.tld)\" <post@kijiji.ca>");
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void frenchLocale_doNotAnonymize_fromNameMissing_headersSet() throws Exception {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("anonymize", "false"));
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("fr_CA");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn(null);
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(SELLER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("Reply-To", BUYER_DOMAIN_TLD);
        verify(outgoingMail).addHeader("From", "=?ISO-8859-1?Q?R=E9ponse_Kijiji_=28de_buyer=40domain=2Etld=29?= <post@kijiji.ca>");
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void english_doNotAnonymize_fromNameSet_headersSet() throws Exception {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of("anonymize", "false"));
        when(context.getMessageDirection()).thenReturn(MessageDirection.BUYER_TO_SELLER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("en_CA");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn("sender");
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(SELLER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("Reply-To", BUYER_DOMAIN_TLD);
        verify(outgoingMail).addHeader("From", "\"Kijiji Reply (from sender)\" <post@kijiji.ca>");
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void conversationAnonymized_followUpMsgWithDoNotAnonymizeFlag_ignoredAndAnonymized() throws Exception {
        when(context.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("en_CA");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn("seller");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_ANONYMIZE)).thenReturn("false");
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(BUYER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("From", "seller" + " <" + SELLER_CLOAKED + ">");
        verifyNoMoreInteractions(outgoingMail);
    }

    @Test
    public void conversationDoesntHaveAnonFlag_anonymized() throws Exception {
        when(conversation.getCustomValues()).thenReturn(ImmutableMap.of());
        when(context.getMessageDirection()).thenReturn(MessageDirection.SELLER_TO_BUYER);
        when(context.getMail()).thenReturn(Optional.of(incomingMail));
        when(incomingMail.getUniqueHeader(Addresser.HEADER_LOCALE)).thenReturn("en_CA");
        when(incomingMail.getUniqueHeader(Addresser.HEADER_FROM_NAME)).thenReturn("seller");
        when(context.getOutgoingMail()).thenReturn(outgoingMail);

        addresser.postProcess(context);

        verify(outgoingMail).setTo(new MailAddress(BUYER_DOMAIN_TLD));
        verify(outgoingMail).addHeader("From", "seller" + " <" + SELLER_CLOAKED + ">");
        verifyNoMoreInteractions(outgoingMail);
    }
}
