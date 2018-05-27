package com.ecg.comaas.gtuk.filter.category;

import java.util.Optional;

public interface CategoryClient extends AutoCloseable {

    Optional<Category> categoryTree();

    Optional<String> version();
}