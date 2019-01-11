package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RemoteFilterConfigurations {
    private final URL remoteEndpoint;
    private final Set<String> remotelyValidatedFilterTypes;

    @Autowired
    public RemoteFilterConfigurations(
            @Value("${comaas.filter.remote.factories:}") String csvListOfFactories,
            @Value("${comaas.filter.remote.endpoint:}") String remoteEndpoint
    ) {
        if (remoteEndpoint.equals("")) {
            this.remoteEndpoint = null;
        } else {
            try {
                this.remoteEndpoint = URI.create(remoteEndpoint).toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        remotelyValidatedFilterTypes = Arrays.stream(csvListOfFactories.split(","))
                .collect(Collectors.toSet());
    }

    public Optional<URL> getRemoteEndpoint(PluginConfiguration pluginConf) {
        String factoryName = pluginConf.getId().getPluginFactory();
        if (remotelyValidatedFilterTypes.contains(factoryName)) {
            return Optional.ofNullable(remoteEndpoint);
        }
        return Optional.empty();
    }


}
