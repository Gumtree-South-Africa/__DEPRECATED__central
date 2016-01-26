package com.ecg.replyts.client.configclient;

import com.ecg.replyts.core.api.pluginconfiguration.PluginState;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReplyTsConfigClient {

    private final int port;

    public ReplyTsConfigClient(int port) {
        this.port = port;
    }

    private String configBase() {
        return "http://localhost:" + port + "/configv2/";
    }

    /**
     * returns all configurations, ReplyTS knows of. You can assume that each configuration is associated to a filter
     * running inside ReplyTS processing mails.<br/>
     * Ths call may fail if network errors occur. in that case, a runtime exception is thrown.
     */
    public List<Configuration> listConfigurations() {
        try {
            String data = Request.Get(configBase())
                    .execute().returnContent().asString();
            ArrayNode an = (ArrayNode) JsonObjects.parse(data).get("configs");

            List<Configuration> res = new ArrayList<>();
            for (int i = 0; i < an.size(); i++) {
                ObjectNode on = (ObjectNode) an.get(i);
                res.add(transformToConfig(on));
            }
            return res;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * removes a filter configuration, and removes the associated filter from the filter chain asynchronousely.<br/>
     * This call may fail, if network errors occur or the specified configuration does not exist. In that case,
     * an exception is thrown.
     */
    public void deleteConfiguration(Configuration.ConfigurationId configurationId) {
        String path = buildPathForConfigId(configurationId);
        try {
            Request.Delete(path).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildPathForConfigId(Configuration.ConfigurationId configurationId) {
        return configBase() + configurationId.getPluginFactory() + "/" + configurationId.getInstanceId();
    }

    /**
     * adds or updates a filter configuration and starts a new filter. The call will block until the filter is put into
     * action on the ReplyTS node that received this API call. All other ReplyTS nodes will be informed about a filter
     * change asynchronousely but shortly after.
     * <p/>
     * <br/>This call may fail, if:
     * <ol>
     * <li>Network errors occur</li>
     * <li>The configuration points to a nonexistant Filter Factory</li>
     * <li>The configuration is unparsable or rejected by the filter factoy</li>
     * </ol>
     * in any of that case, a RuntimeException is thrown.
     */
    public void putConfiguration(Configuration config) {
        String payload = JsonObjects.builder()
                .attr("state", config.getState().name())
                .attr("priority", config.getPriority())
                .attr("configuration", config.getConfiguration()).toJson();
        String path = buildPathForConfigId(config.getConfigurationId());

        try {
            String data = Request.Put(path)
                    .addHeader("Content-Type", "application/json")
                    .body(new StringEntity(payload))
                    .execute().returnContent().asString();

            JsonNode result = JsonObjects.parse(data);
            boolean success = result.get("state").textValue().equals("OK");
            if (!success) {
                throw new RuntimeException("Failed to execute. Message: " + result.get("message").textValue());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Configuration transformToConfig(ObjectNode on) {
        String pluginFactory = on.get("pluginFactory").textValue();
        String instanceId = on.get("instanceId").textValue();
        int prio = on.get("priority").intValue();
        PluginState s = PluginState.valueOf(on.get("state").textValue());
        return new Configuration(new Configuration.ConfigurationId(pluginFactory, instanceId), s, prio, on.get("configuration"));
    }


}
