package com.ecg.comaas.gtuk.filter.category;

import java.util.*;
import java.util.function.Consumer;

public class DefaultCategoryService implements CategoryService {

    private static final State EMPTY_STATE = new State(new Category(), Collections.emptyMap());

    private State internal;

    private DefaultCategoryService(State state) {
        this.internal = state;
    }

    @Override
    public List<Category> getHierarchy(long id) {
        List<Category> path = new ArrayList<>();
        Category category = internal.idToCategory.get(id);
        if (category != null) {
            walkTreeDownUp(category, c -> {
                if (!c.equals(internal.category)) {
                    path.add(c);
                }
            });
        }

        return path;
    }

    @Override
    public List<Category> getFullPath(long id) {
        List<Category> path = new ArrayList<>();
        Category category = internal.idToCategory.get(id);
        if (category != null) {
            walkTreeDownUp(category, c -> path.add(c));
        }

        return path;
    }

    private void walkTreeDownUp(Category node, Consumer<Category> consumer) {
        Optional<Category> parent = getParent(node);
        if (parent.isPresent()) {
            walkTreeDownUp(parent.get(), consumer);
        }

        consumer.accept(node);
    }

    private Optional<Category> getParent(Category category) {
        if (category != internal.category && category.getParentId() != null) {
            Category parent = internal.idToCategory.get(category.getParentId());
            return Optional.ofNullable(parent);
        }

        return Optional.empty();
    }

    private static void processNodes(Category node, Integer level, Map<Long, Category> idToCategory) {
        idToCategory.put(node.getId(), node);
        node.setDepth(level);

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            node.getChildren().forEach(c -> processNodes(c, level + 1, idToCategory));
        }
    }

    @Override
    public void reload(Category newCategory) {
        if (newCategory != null && !newCategory.equals(internal.category)) {
            this.internal = createNewState(newCategory);
        }
    }

    public static DefaultCategoryService createNewService() {
        return new DefaultCategoryService(EMPTY_STATE);
    }

    private static State createNewState(Category category) {
        Map<Long, Category> idToCategory = new HashMap<>();
        processNodes(category, 0, idToCategory);
        return new State(category, idToCategory);
    }

    private static class State {
        private final Category category;
        private final Map<Long, Category> idToCategory;

        private State(Category category,  Map<Long, Category> idToCategory) {
            this.category = category;
            this.idToCategory = idToCategory;
        }
    }
}