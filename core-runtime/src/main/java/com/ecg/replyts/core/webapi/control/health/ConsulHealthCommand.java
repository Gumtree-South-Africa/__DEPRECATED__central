package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Self;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

public class ConsulHealthCommand extends AbstractHealthCommand {

    private ConsulClient consul;

    ConsulHealthCommand(ConsulClient consul) {
        this.consul = consul;
    }

    @Override
    public ObjectNode execute() {
        try {
            Response<Self> self = consul.getAgentSelf();
            Self.Config config = self.getValue().getConfig();
            Response<Map<String, List<String>>> services =
                    consul.getCatalogServices(QueryParams.DEFAULT);

            ObjectNode detail = JsonObjects.builder()
                    .attr("services", services.getValue().toString())
                    .attr("advertiseAddress", config.getAdvertiseAddress())
                    .attr("datacenter", config.getDatacenter())
                    .attr("domain", config.getDomain())
                    .attr("nodeName", config.getNodeName())
                    .attr("bindAddress", config.getBindAddress())
                    .attr("clientAddress", config.getClientAddress())
                    .build();

            return JsonObjects.builder()
                    .attr(STATUS, Status.UP.name())
                    .attr("detail", detail)
                    .build();
        } catch (Exception e) {
            return status(Status.DOWN, e.getMessage());
        }
    }

    @Override
    public String name() {
        return "consul";
    }
}
