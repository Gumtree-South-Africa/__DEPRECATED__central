package com.gumtree.comaas.filter.category;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.api.category.CategoryModel;
import com.gumtree.api.category.domain.Category;
import com.gumtree.filters.comaas.config.CategoryFilterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is not actually a filter that filters anything, it just adds a category to be consumed by filters down the chain.
 */
@Component
public class GumtreeCategoryBreadcrumbFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeCategoryBreadcrumbFilter.class);
    private Timer timer = TimingReports.newTimer("category-process-time");

    private static final String CATEGORYID = "categoryid";

    private CategoryModel categoryModel;

    private CategoryFilterConfig filterConfig;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) throws ProcessingTimeExceededException {
        if (filterConfig.isActive()) {
            try (Timer.Context ignore = timer.time()) {
                final Long categoryId = Long.valueOf(context.getConversation().getCustomValues().get(CATEGORYID));
                List<Category> fullPath = categoryModel.getFullPath(categoryId);
                Set<Long> collect = fullPath.parallelStream().map(Category::getId).collect(Collectors.toSet());
                Set<Long> categoryBreadCrumb = (Set<Long>) context.getFilterContext().get("categoryBreadCrumb");
                categoryBreadCrumb.addAll(collect);
            } catch (NumberFormatException nEx) {
                LOG.debug("Number format exception while parsing category id: " + nEx);
            }
        }
        return Collections.emptyList();
    }

    public GumtreeCategoryBreadcrumbFilter withFilterConfig(CategoryFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        return this;
    }

    GumtreeCategoryBreadcrumbFilter withCategoryModel(CategoryModel categoryModel) {
        this.categoryModel = categoryModel;
        return this;
    }
}
