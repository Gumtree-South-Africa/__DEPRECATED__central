package com.ecg.comaas.mde.postprocessor.addmetadata;

import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.integration.test.MailInterceptor;
import com.ecg.replyts.integration.test.MailBuilder;
import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

public class MailAddMetadataIntegrationTest {
    @Rule
    public ReplyTsIntegrationTestRule replyTsIntegrationTestRule = new ReplyTsIntegrationTestRule(((Supplier<Properties>) () -> {
        Properties properties = new Properties();

        properties.put("replyts.mobilede.addmetadata.plugin.order", "201" );

        return properties;
    }).get());

    @Test
    public void testAddingMetadata() {
        MailBuilder mailBuilder = MailBuilder.aNewMail()
                .adId("4711")
                .htmlBody("<html> foo $CONVERSATION_ID$ bar $MESSAGE_ID$ foo bar $ANONYMIZED_SENDER_ADDRESS$ bar</html>")
                .plainBody("foo $CONVERSATION_ID$ bar $MESSAGE_ID$ foo bar $ANONYMIZED_SENDER_ADDRESS$ bar")
                .from("buyer@example.com")
                .to("seller@example.com");


        MailInterceptor.ProcessedMail processedMail = replyTsIntegrationTestRule.deliver(mailBuilder);

        String conversationId = processedMail.getConversation().getId();

        String messageId = processedMail.getMessage().getId();

        List<TypedContent<String>> textParts = processedMail.getOutboundMail().getTextParts(false);

        assertEquals(2, textParts.size());

        for(TypedContent<String> content : textParts) {
            if(content.getMediaType().toString().equals("text/html")) {
                assertEquals(String.format("<html> foo %s bar %s foo bar %s bar</html>", conversationId, messageId, processedMail.getOutboundMail().getFrom()), content.getContent());
            } else {
                assertEquals(String.format("foo %s bar %s foo bar %s bar", conversationId, messageId, processedMail.getOutboundMail().getFrom()), content.getContent());
            }
        }



    }

}
