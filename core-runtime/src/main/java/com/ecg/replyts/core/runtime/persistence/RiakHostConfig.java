package com.ecg.replyts.core.runtime.persistence;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

/**
 * Contains all information required for connecting to Riak. Host Configuration is multi datacenter aware and
 * reads it's configuration from properties files.
 * <p/>
 * For dual datacenter configuration the primary datacenter is the same as the server runs in. RTS should only
 * connect to the same (primary) datacenter.
 * <pre>
 *     persistence.riak.datacenter.primary.hosts
 * </pre>
 * <p/>
 *
 * @author mhuttar
 */
@Component
public class RiakHostConfig {
    private final List<Host> hostList;

    private final int protobufPort;
    private final int httpPort;

    /**
     * Describes a Riak host.
     * <p/>
     * Need hasCode/equals to be able to use the host as key in maps.
     */
    public static class Host {
        private final String host;
        private final int protobufPort;

        private Host(String host, int protobufPort) {
            this.host = host;
            this.protobufPort = protobufPort;
        }

        public String getHost() {
            return host;
        }

        @Override
        public String toString() {
            return format("%s:%d", host, protobufPort);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Host)) {
                return false;
            }
            Host otherHost = (Host) other;
            return host.equals(otherHost.host)
                    && protobufPort == otherHost.protobufPort;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(host, protobufPort);
        }
    }

    @Autowired
    public RiakHostConfig(@Value("${persistence.riak.datacenter.primary.hosts:}")
                          String riakHostsInPrimaryDatacenter,
                          @Value("${persistence.riak.pb.port:8087}")
                          int protobufPort,
                          @Value("${persistence.riak.http.port:8098}")
                          int httpPort) {

        this.protobufPort = protobufPort;
        this.httpPort = httpPort;

        // primary host config is mandatory
        Preconditions.checkArgument(!isNullOrEmpty(riakHostsInPrimaryDatacenter));

        // host list for primary datacenter must be configured
        hostList = parseHostList(riakHostsInPrimaryDatacenter, protobufPort, httpPort);
    }

    public List<Host> getHostList() {
        return hostList;
    }

    private static List<Host> parseHostList(String hostList, int protobufPort, int httpPort) {
        List<Host> hosts = newArrayList();
        for (String part : Splitter.on(',').omitEmptyStrings().split(hostList)) {
            Host host = new Host(part, protobufPort);
            hosts.add(host);
        }
        return hosts;
    }

    public int getProtobufPort() {
        return protobufPort;
    }

    public int getHttpPort() {
        return httpPort;
    }
}