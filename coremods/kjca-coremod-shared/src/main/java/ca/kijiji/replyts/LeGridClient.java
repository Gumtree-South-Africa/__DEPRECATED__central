package ca.kijiji.replyts;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.util.Map;

public class LeGridClient {

    private final Client client;
    private final String gridApiEndPoint;

    public LeGridClient(String gridApiEndPoint, String gridApiUser, String gridApiPassword) {

        this.gridApiEndPoint = gridApiEndPoint;

        ClientConfig clientConfig = new ClientConfig();
        clientConfig.property(ClientProperties.READ_TIMEOUT, 5000);
        clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 500);

        client = ClientBuilder.newClient(clientConfig);
        client.register(JacksonFeature.class);

        HttpAuthenticationFeature authentication = HttpAuthenticationFeature.basic(gridApiUser, gridApiPassword);
        client.register(authentication);
    }

    public Client getClient() {
        return client;
    }

    public String getGridApiEndPoint() {
        return gridApiEndPoint;
    }

    public Map getJsonAsMap(String path) {
        return client.target(gridApiEndPoint)
                .path(path)
                .request(MediaType.APPLICATION_JSON)
                .get(Map.class);
    }

}
