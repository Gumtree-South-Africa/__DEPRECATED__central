package com.ecg.comaas.gtuk.filter.category;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.command.AddCustomValueCommand;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class adds the categories in the breadcrumb as events/commands to the Conversation,
 * so they are available on the Conversation level.
 * It must be called from a filter that is always active. The CategoryBreadcrumb filter adds the category to the
 * MessageProcessingContext, is second in line in the priority list, and is always active, according to GTUK.
 */
class CategoryPreProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(CategoryPreProcessor.class);

    private static final String CATEGORYID_KEY = "categoryid";

    static void addCategoriesToConversation(CategoryService categoryService, MessageProcessingContext context) {
        Conversation conversation = context.getConversation();
        String categoryId = conversation.getCustomValues().get(CATEGORYID_KEY);
        if (categoryId == null) {
            LOG.info("Could not find CustomValue {} for conversation, unable to save category hierarchy to event stream", CATEGORYID_KEY);
            return;
        }

        List<Category> hierarchy = categoryService.getHierarchy(Long.valueOf(categoryId));

        hierarchy.forEach(category -> {
            String categoryDepthKey = String.format("l%d-categoryid", category.getDepth());
            String existingCustomValue = context.getConversation().getCustomValues().get(categoryDepthKey);
            String newValue = String.valueOf(category.getId());
            if (existingCustomValue == null || !existingCustomValue.equals(newValue)) {
                context.addCommand(new AddCustomValueCommand(conversation.getId(), categoryDepthKey, newValue));
            }
        });
    }
}
