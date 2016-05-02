package com.ecg.de.ebayk.messagecenter.cleanup;

import com.ecg.de.ebayk.messagecenter.cleanup.TextCleaner;
import com.ecg.de.ebayk.messagecenter.util.MessageIdHeaderEncryption;
import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pragone on 17/04/15.
 */
public class MessageCleanupTest {

    private ObjectMapper objectMapper = new ObjectMapper();
    private static MessageIdHeaderEncryption MESSAGE_HEADER_ENCRYPTION = new MessageIdHeaderEncryption();

    public Mail readMail(String conversationId, String messageId) throws IOException, ParsingException {
        InputStream fin = getClass().getResourceAsStream("/corpus/" + conversationId + "/" + messageId);
        Mail mail = new Mails().readMail(ByteStreams.toByteArray(fin));
        return mail;
    }

    public Map readConversation(String conversationId) throws IOException {
        InputStream fin = getClass().getResourceAsStream("/corpus/" + conversationId + "/conversation");
        return this.objectMapper.readValue(fin, Map.class);
    }

    @Test
    @Ignore
    public void testDecrypt() {
        System.out.println(MESSAGE_HEADER_ENCRYPTION.decrypt("1huavi7ynbfgewm6u7qucu89h0s"));
    }

    public SmallConversation loadConversation(String conversationId) throws Exception {
        Map conversation =  readConversation(conversationId);
        Map<String, Object> body = (Map<String, Object>) conversation.get("body");
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");

        SmallConversation response = new SmallConversation();
        response.setConversationId(conversationId);

        for (Map<String, Object> message : messages) {
            String messageId = message.get("id").toString();
            response.addMessage(messageId, readMail(conversationId, messageId), MessageDirection.valueOf(message.get("messageDirection").toString()));
        }

        return response;
    }

    @Test
    @Ignore
    public void testOneConversation() throws Exception {
        System.out.println(doTest("2a94j:i8bdi7zl"));;
    }


    @Test
    @Ignore
    public void testAllConversation() throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/corpus/conversation_list")));
        String line;
        while ((line = br.readLine()) != null) {
            String resp = doTest(line.trim());
            File file = new File("/tmp/results/" + line);
            FileOutputStream fos = new FileOutputStream(file);
            IOUtils.write(resp, fos);
            fos.close();
        }
    }

    private String doTest(String conversationId) throws Exception {
        SmallConversation conversation = loadConversation(conversationId);
        long start = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("Conversation " + conversationId + "\n");
        for (String messageId : conversation.getMessageIds()) {
            long messageStart = System.currentTimeMillis();
            MailWrapper mail = conversation.getMail(messageId);
            String finalCleanup = mail.getText();
            finalCleanup = TextCleaner.cleanupText(finalCleanup);
            long messageEnd = System.currentTimeMillis();
            sb.append("******* " + mail.getMessageDirection() + " ******* (" + (messageEnd - messageStart) + " / " + mail.getMessageId() + ")\n");
            sb.append(finalCleanup + "\n");
        }
        sb.append("Done in " + (System.currentTimeMillis() - start));
        return sb.toString();
    }

    private class SmallConversation {
        private Map<String,Object> status;
        private String conversationId;
        private Map<String, MailWrapper> mails = new HashMap<>();
        private List<String> mailOrder = new ArrayList<>();

        public Map<String, Object> getStatus() {
            return status;
        }

        public void setStatus(Map<String, Object> status) {
            this.status = status;
        }

        public String getConversationId() {
            return conversationId;
        }

        public void setConversationId(String conversationId) {
            this.conversationId = conversationId;
        }

        public void addMessage(String messageId, Mail mail, MessageDirection messageDirection) {
            this.mailOrder.add(messageId);
            this.mails.put(messageId, new MailWrapper(messageId, mail, messageDirection));
        }

        public MailWrapper getMail(String messageId) {
            return this.mails.get(messageId);
        }

        public MailWrapper getMail(int index) {
            return getMail(this.mailOrder.get(index));
        }

        public List<String> getMessageIds() {
            return this.mailOrder;
        }
    }

    public static class MailWrapper {
        private final String messageId;
        private final Mail mail;
        private final MessageDirection messageDirection;

        public MailWrapper(String messageId, Mail mail, MessageDirection messageDirection) {
            this.messageId = messageId;
            this.mail = mail;
            this.messageDirection = messageDirection;
        }

        public String getMessageId() {
            return messageId;
        }

        public Mail getMail() {
            return mail;
        }

        public String getInResponseToMessageId() {
            String header = mail.getUniqueHeader("In-Reply-To");
            if (header != null) {
                if (header.startsWith("<")) {
                    header = header.substring(1);
                }
                String[] parts = header.split("@", 2);
                try {
                    return MESSAGE_HEADER_ENCRYPTION.decrypt(parts[0]);
                } catch (Exception e) {
                    //System.err.println("Couldn't get InResponseTo: " + header);
                }
            }
            return null;
        }

        public MessageDirection getMessageDirection() {
            return messageDirection;
        }

        public String getText() {
            return mail.getPlaintextParts().get(0);
        }

        public boolean isStarter() {
            return this.mail.getUniqueHeader("X-ADID") != null;
        }
    }
}
