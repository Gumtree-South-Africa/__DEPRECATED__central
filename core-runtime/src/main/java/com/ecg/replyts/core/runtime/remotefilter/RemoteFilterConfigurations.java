package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.replyts.core.api.configadmin.PluginConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RemoteFilterConfigurations {

    private final URI remoteBaseEndpoint;
    private final Set<String> remotelyValidatedFilterTypes;

    @Autowired
    public RemoteFilterConfigurations(
            @Value("${comaas.filter.remote.factories:}") String csvListOfFactories,
            @Value("${comaas.filter.remote.endpoint:}") String remoteEndpoint
    ) {
        if (remoteEndpoint.equals("")) {
            this.remoteBaseEndpoint = null;
        } else {
            this.remoteBaseEndpoint = URI.create(remoteEndpoint);

            // check early to see if it is a url
            try {
                URI.create(remoteEndpoint).toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        remotelyValidatedFilterTypes = Arrays.stream(csvListOfFactories.split(","))
                .collect(Collectors.toSet());
    }

    public static RemoteFilterConfigurations createEmptyConfiguration() {
        return new RemoteFilterConfigurations("", "");
    }

    public Optional<URL> getRemoteEndpoint(PluginConfiguration pluginConf) {
        String factoryName = pluginConf.getId().getPluginFactory();
        if (remotelyValidatedFilterTypes.contains(factoryName)) {
            URI endpoint = UriBuilder.fromUri(remoteBaseEndpoint)
                    .path(pluginConf.getUuid().toString())
                    .build();
            try {
                return Optional.ofNullable(endpoint.toURL());
            } catch (MalformedURLException e) {
                new RuntimeException("Failed to convert URI to URL. This is a bug: should have been prevented with earlier validation.");
            }
        }
        return Optional.empty();
    }
}
