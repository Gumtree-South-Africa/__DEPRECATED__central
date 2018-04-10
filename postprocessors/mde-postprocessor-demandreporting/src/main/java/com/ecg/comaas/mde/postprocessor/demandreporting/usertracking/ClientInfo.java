package com.ecg.comaas.mde.postprocessor.demandreporting.usertracking;

import java.util.Collections;
import java.util.Map;

public class ClientInfo {
    
    final String version;
    final String apiVersion;
    final String ip;
    final String deviceType;
    final String userAgent;
    final String akamaiBot;
    final String name;
    final Map<String, String> experiments;



    ClientInfo(String version, String apiVersion, String ip, String deviceType, String userAgent, String akamaiBot, String name, Map<String, String> experiments) {
        this.version = version;
        this.apiVersion = apiVersion;
        this.ip = ip;
        this.deviceType = deviceType;
        this.userAgent = userAgent;
        this.akamaiBot = akamaiBot;
        this.name = name;
        this.experiments = experiments;
    }

    public static ClientInfoBuilder builder() {
        return new ClientInfoBuilder();
    }

    public static final class ClientInfoBuilder {
        String version;
        String apiVersion;
        String ip;
        String deviceType;
        String userAgent;
        String akamaiBot;
        String name;
        Map<String, String> experiments = Collections.emptyMap();

        private ClientInfoBuilder() {
        }



        public ClientInfoBuilder withVersion(String version) {
            this.version = version;
            return this;
        }

        public ClientInfoBuilder withApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public ClientInfoBuilder withIp(String ip) {
            this.ip = ip;
            return this;
        }

        public ClientInfoBuilder withDeviceType(String deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public ClientInfoBuilder withUserAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public ClientInfoBuilder withAkamaiBot(String akamaiBot) {
            this.akamaiBot = akamaiBot;
            return this;
        }
        
        public ClientInfoBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ClientInfoBuilder withExperiments(Map<String, String> experiments) {
            if (experiments != null) {
                this.experiments = experiments;
            }
            return this;
        }

        public ClientInfo build() {
            return new ClientInfo(version, apiVersion, ip, deviceType, userAgent, akamaiBot, name, experiments);
        }
    }
}
