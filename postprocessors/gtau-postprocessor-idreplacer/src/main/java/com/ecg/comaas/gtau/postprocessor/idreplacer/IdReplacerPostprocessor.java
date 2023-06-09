package com.ecg.comaas.gtau.postprocessor.idreplacer;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTAU;

@ComaasPlugin
@Profile(TENANT_GTAU)
@Component
public class IdReplacerPostprocessor implements PostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(IdReplacerPostprocessor.class);
    private static final Pattern CONVERSATION_ID = Pattern.compile("<%%CONVERSATION_ID%%>");
    private static final Pattern MESSAGE_ID = Pattern.compile("<%%MESSAGE_ID%%>");
    private static final Pattern HASH = Pattern.compile("<%%HASH%%>");
    private static final String ALGORITHM = "HmacMD5";
    private static final Mac HMAC_MD5;
    private static final String SECRET_SALT = "X23!=?m(";

    static {
        try {
            HMAC_MD5 = Mac.getInstance(ALGORITHM);
            final SecretKeySpec key = new SecretKeySpec(SECRET_SALT.getBytes(), ALGORITHM);
            HMAC_MD5.init(key);
        } catch (InvalidKeyException e) {
            String errorMessage = "Invalid Key: " + e.getMessage();
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        } catch (NoSuchAlgorithmException e) {
            String errorMessage = "MD5 provider not found: " + e.getMessage();
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage);
        }
    }

    private final int order;

    @Autowired
    public IdReplacerPostprocessor(@Value("${replyts-id-replacer.plugin.order:250}") int order) {
        this.order = order;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        final String messageId = context.getMessage().getId();
        final String conversationId = context.getConversation().getId();
        final String adId = context.getConversation().getAdId();
        final List<TypedContent<String>> tsc = context.getOutgoingMail().getTextParts(false);

        for (final TypedContent<String> c : tsc) {
            String output = c.getContent();

            final boolean replaceMessageId = MESSAGE_ID.matcher(output).find();
            final boolean replaceConversationId = CONVERSATION_ID.matcher(output).find();
            if (!replaceConversationId && !replaceMessageId) {
                continue;
            }

            final String hashInput = adId
                    + (replaceConversationId ? conversationId : "")
                    + (replaceMessageId ? messageId : "");

            LOG.debug("Hash input [{}]", hashInput);

            output = CONVERSATION_ID.matcher(output).replaceAll(conversationId);
            output = MESSAGE_ID.matcher(output).replaceAll(messageId);
            output = HASH.matcher(output).replaceAll(hash(hashInput));

            c.overrideContent(output);
        }
    }

    private static String hash(final String source) {
        final byte[] hash = HMAC_MD5.doFinal(source.getBytes());
        return Base64.encodeBase64URLSafeString(hash);
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
