package com.ecg.gumtree.comaas.common.filter;

import com.ecg.gumtree.comaas.common.domain.CommonConfig;
import com.ecg.gumtree.comaas.common.domain.State;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class GumtreeFilterFactory<T extends CommonConfig, R extends com.ecg.replyts.core.api.pluginconfiguration.filter.Filter> implements FilterFactory {
    private final Class<T> type;
    private final BiFunction<Filter, T, R> createConfig;
    private final BiConsumer<String, T> callInit;

    public GumtreeFilterFactory(Class<T> type, BiFunction<Filter, T, R> createConfig) {
        this(type, createConfig, null);
    }

    public GumtreeFilterFactory(Class<T> type, BiFunction<Filter, T, R> createConfig, BiConsumer<String, T> callInit) {
        this.type = type;
        this.createConfig = createConfig;
        this.callInit = callInit;
    }

    @Nonnull
    @Override
    public final com.ecg.replyts.core.api.pluginconfiguration.filter.Filter createPlugin(String instanceName, JsonNode configuration) {
        String name = this.getClass().getName();
        Filter pluginConfig = new Filter(name, instanceName, configuration);
        T filterConfig;
        try {
            filterConfig = ConfigMapper.asObject(configuration.toString(), type);
        } catch (IOException e) {
            throw new IllegalStateException("Could not convert config to object", e);
        }

        if (filterConfig.getState() == State.DISABLED) {
            return new DisabledFilter(this.getClass());
        }
        if (this.callInit != null) {
            this.callInit.accept(instanceName, filterConfig);
        }

        return createConfig.apply(pluginConfig, filterConfig);
    }

    @Override
    abstract public String getIdentifier();
}
