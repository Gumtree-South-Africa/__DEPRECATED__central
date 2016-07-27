package com.ecg.messagecenter.persistence.block;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import static com.ecg.messagecenter.util.ConverterUtils.nullSafeMillis;

@Component
@ConditionalOnExpression("#{'${persistence.strategy}' == 'riak' || '${persistence.strategy}' == 'hybrid'}")
public class ConversationBlockToJsonConverter {
    public String toJson(ConversationBlock conversationBlock) {
        JsonObjects.Builder builder = JsonObjects.builder()
                .attr("version", conversationBlock.getVersion());

        if (conversationBlock.getBuyerBlockedSellerAt().isPresent()) {
            builder.attr("buyerBlockedSellerAt", nullSafeMillis(conversationBlock.getBuyerBlockedSellerAt().get()));
        }
        if (conversationBlock.getSellerBlockedBuyerAt().isPresent()) {
            builder.attr("sellerBlockerBuyerAt", nullSafeMillis(conversationBlock.getSellerBlockedBuyerAt().get()));
        }

        return builder.toJson();
    }
}
