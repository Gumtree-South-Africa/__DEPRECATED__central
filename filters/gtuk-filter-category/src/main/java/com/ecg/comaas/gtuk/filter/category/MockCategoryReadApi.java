package com.ecg.comaas.gtuk.filter.category;

import com.gumtree.api.category.CategoryReadApi;
import com.gumtree.api.category.domain.AttributeMetadata;
import com.gumtree.api.category.domain.Category;
import com.gumtree.api.category.domain.VipMetadata;
import com.gumtree.api.common.Versioned;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class MockCategoryReadApi implements CategoryReadApi {

    @Override
    public Versioned<Category> categoryTree() {
        return versionedMock(createMockCategory());
    }

    private Category createMockCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setDepth(0);
        category.setChildren(Collections.emptyList());
        return category;
    }

    private <T> Versioned<T> versionedMock(T object) {
        return new Versioned<>("1.0", object);
    }

    @Override
    public Versioned<Collection<AttributeMetadata>> categoryAttributes() {
        return versionedMock(Collections.emptyList());
    }

    @Override
    public Versioned<Collection<VipMetadata>> categoryVipMetadata() {
        return versionedMock(Collections.emptyList());
    }

    @Override
    public Collection<AttributeMetadata> attributeMetadataForCategory(Long aLong) {
        return Collections.emptyList();
    }

    @Override
    public Category virtualCategoryTree() {
        return createMockCategory();
    }

    @Override
    public Versioned<Category> categoryTreeFilterOut(Map<String, Object> map) {
        return versionedMock(createMockCategory());
    }

    @Override
    public String version() {
        return "1.0";
    }

    @Override
    public Category getCategoryById(Long aLong) {
        return createMockCategory();
    }
}
