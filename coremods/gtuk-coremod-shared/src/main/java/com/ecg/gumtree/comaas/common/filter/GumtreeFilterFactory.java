package com.ecg.gumtree.comaas.common.filter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.gumtree.filters.comaas.config.CommonConfig;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.json.ConfigMapper;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class GumtreeFilterFactory<T extends CommonConfig, R extends Filter> implements FilterFactory {
    private final Class<T> type;
    private final BiFunction<com.gumtree.filters.comaas.Filter, T, R> createConfig;
    private final BiConsumer<String, T> callInit;

    public GumtreeFilterFactory(Class<T> type, BiFunction<com.gumtree.filters.comaas.Filter, T, R> createConfig) {
        this(type, createConfig, null);
    }

    public GumtreeFilterFactory(Class<T> type, BiFunction<com.gumtree.filters.comaas.Filter, T, R> createConfig, BiConsumer<String, T> callInit) {
        this.type = type;
        this.createConfig = createConfig;
        this.callInit = callInit;
    }

    @Nonnull
    @Override
    public final Filter createPlugin(String instanceName, JsonNode configuration) {
        String name = this.getClass().getName();
        com.gumtree.filters.comaas.Filter pluginConfig = new com.gumtree.filters.comaas.Filter(name, instanceName, configuration);
        T filterConfig = ConfigMapper.asObject(configuration.toString(), type);

        if (filterConfig.getState() == State.DISABLED) {
            return new DisabledFilter(this.getClass());
        }
        if (this.callInit != null) {
            this.callInit.accept(instanceName, filterConfig);
        }

        return createConfig.apply(pluginConfig, filterConfig);
    }
}
