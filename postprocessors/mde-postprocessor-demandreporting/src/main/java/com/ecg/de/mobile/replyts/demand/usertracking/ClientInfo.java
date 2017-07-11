package com.ecg.de.mobile.replyts.demand.usertracking;

public class ClientInfo {
    
    final String version;
    final String apiVersion;
    final String ip;
    final String deviceType;
    final String userAgent;
    final String akamaiBot;
    final String name;


    public ClientInfo(String version, String apiVersion, String ip, String deviceType, String userAgent, String akamaiBot, String name) {
        this.version = version;
        this.apiVersion = apiVersion;
        this.ip = ip;
        this.deviceType = deviceType;
        this.userAgent = userAgent;
        this.akamaiBot = akamaiBot;
        this.name = name;
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

        public ClientInfo build() {
            ClientInfo clientInfo = new ClientInfo(version, apiVersion, ip, deviceType, userAgent, akamaiBot, name);
            return clientInfo;
        }
    }
}
