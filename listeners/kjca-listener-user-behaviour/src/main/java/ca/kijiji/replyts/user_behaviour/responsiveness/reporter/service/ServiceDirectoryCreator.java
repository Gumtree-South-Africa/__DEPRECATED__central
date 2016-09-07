package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.consul.DnsConsulCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.UnknownHostException;

@Component
public class ServiceDirectoryCreator {

    private final String host;
    private final int port;

    @Autowired
    public ServiceDirectoryCreator(
            @Value("${consul.host:vm.dev.kjdev.ca}") String host,
            @Value("${consul.port:8600}") int port
    ) {
        this.host = host;
        this.port = port;
    }

    public ca.kijiji.discovery.ServiceDirectory newServiceDirectory() {
        try {
            return DnsConsulCatalog.usingUdp(this.host, this.port);
        } catch (final UnknownHostException ex) {
            throw new Error("Unable to create a service catalog.", ex);
        }
    }
}
