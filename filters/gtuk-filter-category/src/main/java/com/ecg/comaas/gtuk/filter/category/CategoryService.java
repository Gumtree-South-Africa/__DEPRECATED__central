package com.ecg.comaas.gtuk.filter.category;

import java.util.*;
import java.util.function.Consumer;

public interface CategoryService extends Reloadable<Category> {

    List<Category> getHierarchy(long id);

    List<Category> getFullPath(long id);

}