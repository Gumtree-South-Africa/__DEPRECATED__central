package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentConfiguration;
import com.ecg.replyts.core.runtime.persistence.attachment.AttachmentRepository;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.google.common.io.CharStreams;
import com.google.common.net.MediaType;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HybridMailRepositoryTest.TestContext.class)
public class HybridMailRepositoryTest {

    @MockBean
    private DiffingRiakMailRepository riakMailRepo;

    @MockBean
    private AttachmentRepository attachmentRepository;

    @MockBean
    private SwiftAttachmentRepository swiftAttachmentRepository;

    @Autowired
    private HybridMailRepository hybridMailRepository;

    private final String msgid = "msgid";
    private final String aname = "Screen Shot 2013-08-23 at 10.09.19.png";

    @Test
    public void testPersistAttachmentsForNewMessage() {
        byte[] data = readMail();
        Mail mail = parse();
        String msgid = this.msgid + System.nanoTime();
        when(swiftAttachmentRepository.getNames(msgid)).thenReturn(Optional.empty());
        when(attachmentRepository.hasAttachments(msgid, data)).thenReturn(mail);

        hybridMailRepository.persistMail(msgid, data, null);

        verify(riakMailRepo).doPersist(msgid, data);
        verify(swiftAttachmentRepository).getNames(msgid);
        verify(attachmentRepository).hasAttachments(msgid, data);
        verify(attachmentRepository).storeAttachments(msgid, mail);
    }

    @Test
    public void testMigrateAttachments_MailNoAttachment() {
        byte[] maildata = new byte[]{1, 2, 3};
        when(riakMailRepo.readInboundMail(msgid)).thenReturn(maildata);

        hybridMailRepository.migrateAttachments(msgid);
        verify(attachmentRepository).hasAttachments(msgid, maildata);
        verify(attachmentRepository, never()).storeAttachments(eq(msgid), any());
    }

    @Test
    public void testMigrateAttachments_mailWithAttachment() {
        byte[] maildata = new byte[]{1, 2, 3};
        Mail mail = mock(Mail.class);
        when(riakMailRepo.readInboundMail(msgid)).thenReturn(maildata);
        when(attachmentRepository.hasAttachments(msgid, maildata)).thenReturn(mail);
        when(swiftAttachmentRepository.getNames(msgid)).thenReturn(Optional.empty());

        hybridMailRepository.migrateAttachments(msgid);
        verify(attachmentRepository).hasAttachments(msgid, maildata);
        verify(attachmentRepository).storeAttachments(msgid, mail);
    }

    @Test
    public void testMigrateAttachments_alreadyMigrated() {
        byte[] maildata = new byte[]{1, 2, 3};
        Mail mail = mock(Mail.class);
        when(riakMailRepo.readInboundMail(msgid)).thenReturn(maildata);
        when(attachmentRepository.hasAttachments(msgid, maildata)).thenReturn(mail);
        when(swiftAttachmentRepository.getNames(msgid)).thenReturn(Optional.of(anyMap()));

        hybridMailRepository.migrateAttachments(msgid);
        verify(attachmentRepository).hasAttachments(msgid, maildata);
        verify(swiftAttachmentRepository).getNames(msgid);
        verify(attachmentRepository, never()).storeAttachments(msgid, mail);
    }


    private Mail parse() {
        try {
            return parse(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/mail-with-attachment.eml"))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readMail() {
        try {
            return CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/mail-with-attachment.eml"))).getBytes();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Mail parse(String mailContents) throws ParsingException {
        try {
            return StructuredMail.parseMail(new ByteArrayInputStream(mailContents.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class IsSameTypedContent extends ArgumentMatcher<TypedContent<byte[]>> {
        public boolean matches(Object tc) {
            return Arrays.equals(((TypedContent<byte[]>) tc).getContent(), parse().getAttachment(aname).getContent());
        }
    }


    @Configuration
    @Import({HybridMailConfiguration.class, AttachmentConfiguration.class})
    static class TestContext {

        @Bean
        HybridMigrationClusterState hybridMigrationClusterState() {
            return new HybridMigrationClusterState();
        }

        @Bean
        AttachmentRepository attachmentRepository() {
            return new AttachmentRepository();
        }

        @Bean
        public HybridMailRepository hybridMailRepository() {
            return new HybridMailRepository();
        }

        @Bean
        public HazelcastInstance hazelcastInstance() {
            Config config = new Config();
            config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);

            return Hazelcast.newHazelcastInstance(config);
        }

    }

}
