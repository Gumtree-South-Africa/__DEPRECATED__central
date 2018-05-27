package com.ecg.comaas.gtuk.filter.integration;

import com.ecg.comaas.gtuk.filter.category.Category;
import com.ecg.comaas.gtuk.filter.category.CategoryService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class StubCategoryService implements CategoryService {

    private static long NOT_FOUND_CATEGORY_ID = 404L;

    @Override
    public List<Category> getHierarchy(long id) {
        return Collections.emptyList();
    }

    @Override
    public List<Category> getFullPath(long id) {
        if (NOT_FOUND_CATEGORY_ID == id) {
            return Collections.emptyList();
        }

        return Arrays.asList(100000, 10000, 1000, 100, 10, 1).stream()
                .map(l -> id / l)
                .filter(l -> l > 0)
                .map(StubCategoryService::newCategory)
                .collect(Collectors.toList());
    }

    private static Category newCategory(long id) {
        Category category = new Category();
        category.setId(id);
        category.setSeoName("category-" + id);
        category.setName("Category " + id);
        return category;
    }

    @Override
    public void reload(Category newState) {
    }
}
