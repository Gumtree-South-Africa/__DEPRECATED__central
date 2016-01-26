package com.ecg.replyts.core.runtime.sanitycheck;

import com.ecg.replyts.core.runtime.sanitycheck.adapter.CheckAdapter;

/**
 * Provide the application name by an static configuration. The name will be injected.
 *
 * @author smoczarski
 */
public class StaticApplicationNamingStrategy {

    private final String namespace;

    public StaticApplicationNamingStrategy(String namespace) {
        if (namespace == null || namespace.trim().isEmpty()) throw new IllegalArgumentException("Null not allowed!");
        this.namespace = namespace;
    }


    public String buildJMXName(CheckAdapter adapter) {
        if (adapter == null) throw new IllegalArgumentException("Null not allowed!");

        String name = adapter.getName();
        String subType = getType();

        if (name == null) throw new IllegalArgumentException("Null not allowed!");
        if (subType == null) throw new IllegalArgumentException("Null not allowed!");

        String category = adapter.getCategory();
        String subCategory = adapter.getSubCategory();

        String jmxName = this.namespace;
        jmxName += ":type=SanityChecks";
        jmxName += ",subType=" + subType;
        jmxName += ",path=" + namespace;
        if (category != null) {
            jmxName += ",category=" + category;
        }
        if (subCategory != null) {
            jmxName += ",subCategory=" + subCategory;
        }
        jmxName += ",name=" + name;

        return jmxName;
    }


    /**
     * {@inheritDoc}
     */
    public String getType() {
        return "BackendJob";
    }

}
