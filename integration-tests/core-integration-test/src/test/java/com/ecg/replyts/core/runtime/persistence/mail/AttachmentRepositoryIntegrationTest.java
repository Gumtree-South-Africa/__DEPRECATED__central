package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.ecg.replyts.core.runtime.mailparser.StructuredMail;
import com.ecg.replyts.core.runtime.persistence.attachment.SwiftAttachmentRepository;
import com.google.common.io.CharStreams;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.common.payloads.InputStreamPayload;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;
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
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@Ignore
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AttachmentRepositoryIntegrationTest.TestContext.class)
@TestPropertySource(properties = {
        "swift.bucket.number=13",
        "swift.tenant=comaas-qa",
        "replyts.swift.container=test-container",
        "swift.username=comaas-qa-swift",
        "swift.password=<put password here>",
        "swift.authentication.url=https://keystone.ams1.cloud.ecg.so/v2.0",
})
public class AttachmentRepositoryIntegrationTest {

    @Autowired
    private SwiftAttachmentRepository attachmentRepository;

    private String messageid = "test-message-id";
    private String attName = "Screen Shot 2013-08-23 at 10.09.19.png";
    private int size = 71741;
    private String mimeType = "image/png";

    @Test
    public void writeAttachment() {
        ObjectStorageObjectService so = attachmentRepository.getObjectStorage();
        String containterName = attachmentRepository.getContainer(messageid);
        TypedContent<byte[]> attachment = parse().getAttachment(attName);
        Payload data = new InputStreamPayload(new ByteArrayInputStream(attachment.getContent()));
        ObjectPutOptions options = ObjectPutOptions.create().contentType(mimeType);
        String md5 = so.put(containterName + "/" + messageid, attName, data, options);
        assertEquals("8f1b39be1666c6d66ec20ba754e46dea", md5);
    }

    @Test
    public void getAttachment() {
        SwiftObject so = attachmentRepository.fetch(messageid, attName).get();
        assertEquals(mimeType, so.getMimeType());
        assertEquals(size, so.getSizeInBytes());

    }

    @Test
    public void listAttachmentNames() {
        assertNotNull(attachmentRepository.getNames(messageid).get().get(messageid + "/" + attName));
    }

    // Make sure there is an attachment in the email
    @Test
    public void extractsAttachmentNames() throws IOException {
        Mail mail = parse();
        Files.write(Paths.get(System.getProperty("java.io.tmpdir"), "att.jpeg"), mail.getAttachment(attName).getContent());
        assertEquals(Collections.singletonList(attName), mail.getAttachmentNames());
    }


    // Make sure there is an attachment in the email with correct mime and size
    @Test
    public void retrievesAttachmentContents() {
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

    private Mail parse(String mailContents) throws ParsingException {
        return StructuredMail.parseMail(new ByteArrayInputStream(mailContents.getBytes()));
    }

    @Configuration
    static class TestContext {
        @Bean
        public SwiftAttachmentRepository attachmentRepository() {
            return new SwiftAttachmentRepository();
        }
    }
}
