package com.ecg.comaas.gtuk.filter.category;

import java.util.*;
import java.util.function.Consumer;

public interface CategoryService {

    List<Category> getHierarchy(long id);

    List<Category> getFullPath(long id);

}