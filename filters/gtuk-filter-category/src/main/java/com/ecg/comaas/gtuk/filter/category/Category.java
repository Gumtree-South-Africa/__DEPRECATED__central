package com.ecg.comaas.gtuk.filter.category;

import java.util.List;
import java.util.Objects;

public class Category {

    private Long id;
    private Long parentId;
    private List<Category> children;
    private Integer depth;
    private String name;
    private String seoName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<Category> getChildren() {
        return children;
    }

    public void setChildren(List<Category> children) {
        this.children = children;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSeoName() {
        return seoName;
    }

    public void setSeoName(String seoName) {
        this.seoName = seoName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id) &&
                Objects.equals(parentId, category.parentId) &&
                Objects.equals(children, category.children) &&
                Objects.equals(depth, category.depth) &&
                Objects.equals(name, category.name) &&
                Objects.equals(seoName, category.seoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, parentId, children, depth, name, seoName);
    }
}
