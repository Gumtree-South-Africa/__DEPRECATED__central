package com.ecg.comaas.gtuk.filter.integration;

import com.ecg.comaas.gtuk.filter.category.Category;
import com.ecg.comaas.gtuk.filter.category.CategoryClient;

import java.util.Optional;

public class StubCategoryClient implements CategoryClient {
    @Override
    public Optional<Category> categoryTree() {
        return Optional.empty();
    }

    @Override
    public Optional<String> version() {
        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
    }
}
