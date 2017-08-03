package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.google.common.io.CharStreams;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openstack4j.model.storage.object.SwiftObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AttachmentRepositoryIntegrationTest.TestContext.class)
@TestPropertySource(properties = {
        "swift.tenant=comaas-qa",
        "replyts.swift.container=test-container",
        "swift.username=comaas-qa-swift",
        "swift.password=Z6J$6QfV@HU1%aGv",
})
@Ignore
public class AttachmentRepositoryIntegrationTest {

    @Autowired
    private SwiftAttachmentRepository attachmentRepository;

    private String messageid = "test-message-id";
    private String attName = "Screen Shot 2013-08-23 at 10.09.19.png";
    private int size = 71741;
    private String mimeType = "image/png";
    byte[] rawmail = readMail();

    @Test
    public void getAttachment() {
        SwiftObject so = attachmentRepository.fetch(messageid, attName).get();
        assertEquals(mimeType, so.getMimeType());
        assertEquals(size, so.getSizeInBytes());
    }

    private String getMd5Sum(byte[] data) {
        MessageDigest md5 = DigestUtils.getMd5Digest();
        return Hex.encodeHexString(md5.digest());
    }

    @Test
    public void listAttachmentNames() {
        assertNotNull(attachmentRepository.getNames(messageid).get().get(messageid+"/"+attName));
    }

    // Make sure there is an attachment in the email
    @Test
    public void extractsAttachmentNames() throws IOException, ParsingException {
        Mail mail = parse();
        Files.write( Paths.get(System.getProperty("java.io.tmpdir"),"att.jpeg"), mail.getAttachment(attName).getContent());
        assertEquals(asList(attName), mail.getAttachmentNames());
    }


    // Make sure there is an attachment in the email with correct mime and size
    @Test
    public void retrievesAttachmentContents() throws IOException, ParsingException {
        TypedContent<byte[]> attachment = parse().getAttachment(attName);
        assertEquals(mimeType, attachment.getMediaType().toString());
        assertEquals(size, attachment.getContent().length);
    }

    private Mail parse() {
        try {
            return parse(CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/mails/mail-with-attachment.eml"))));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] readMail() {
        try {
            return CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/mails/mail-with-attachment.eml"))).getBytes();
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

    @Configuration
    static class TestContext {
        @Bean
        public SwiftAttachmentRepository attachmentRepository() {
            return new SwiftAttachmentRepository();
        }
    }
}
