package com.ecg.comaas.gtuk.filter.category;

import com.ecg.gumtree.comaas.common.domain.CategoryFilterConfig;
import com.ecg.gumtree.comaas.common.filter.GumtreeFilterFactory;
import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;

@ComaasPlugin
@Component
@Import(GumtreeCategoryConfiguration.class)
@Profile(TENANT_GTUK)
public class GumtreeCategoryFilterFactory extends GumtreeFilterFactory<CategoryFilterConfig, GumtreeCategoryFilter> {

    private static final String IDENTIFIER = "com.ecg.gumtree.comaas.filter.category.GumtreeCategoryFilterConfiguration$CategoryFilterFactory";

    @Autowired
    public GumtreeCategoryFilterFactory(CategoryService categoryService) {
        super(CategoryFilterConfig.class, (a, b) -> new GumtreeCategoryFilter(categoryService));
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
