package com.ecg.replyts.core.runtime.mailparser;

import com.ecg.replyts.core.api.processing.MessageFixer;
import com.google.common.collect.ImmutableList;
import org.apache.james.mime4j.dom.Message;
import org.apache.james.mime4j.message.DefaultMessageBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.mockito.Mockito.inOrder;

@RunWith(MockitoJUnitRunner.class)
public class StructuredMutableMailTest {
    @Mock
    private MessageFixer fixer1;
    @Mock
    private MessageFixer fixer2;

    @Test
    public void fixersExecutedInOrder() throws Exception {
        List<MessageFixer> fixers = ImmutableList.of(fixer1, fixer2);

        Message message = new DefaultMessageBuilder().parseMessage(new ByteArrayInputStream(
                ("From: foo@bar.com\n" +
                        "To: foo@bar.com\n" +
                        "Delivered-To: foo@bar.com\n" +
                        "Content-Type: text/plain; charset=UTF-8\n" +
                        "Subject: test\n" +
                        "\n" +
                        "test").getBytes()
        ));
        StructuredMail structuredMail = new StructuredMail(message);
        StructuredMutableMail mutableMail = new StructuredMutableMail(structuredMail);

        RuntimeException runtimeException = new RuntimeException();
        mutableMail.applyOutgoingMailFixes(fixers, runtimeException);

        InOrder inOrder = inOrder(fixer1, fixer2);
        inOrder.verify(fixer1).applyIfNecessary(structuredMail.getOriginalMessage(), runtimeException);
        inOrder.verify(fixer2).applyIfNecessary(structuredMail.getOriginalMessage(), runtimeException);
    }
}
