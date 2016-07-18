package com.ecg.replyts.app.search.elasticsearch;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.node.Node;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.TimeUnit;

class ElasticSearchSanityCheck implements Check {

    private final AdminClient admin;
    @Value("${search.es.indexname:replyts}")
    private String indexName;
    
    public ElasticSearchSanityCheck(Node node) {
        admin = node.client().admin();
    }

    @Override
    public Result execute() throws Exception {
        ClusterHealthStatus status = admin.cluster().health(new ClusterHealthRequest(indexName)).get(10, TimeUnit.SECONDS).getStatus();
        Message msg = Message.shortInfo("Cluster State: " + status.name());
        switch (status) {
            case GREEN:
                return Result.createResult(Status.OK, msg);
            case RED:
                return Result.createResult(Status.CRITICAL, msg);
            default:
                return Result.createResult(Status.WARNING, msg);
        }
    }

    @Override
    public String getName() {
        return "ClusterState";
    }

    @Override
    public String getCategory() {
        return "ES";
    }

    @Override
    public String getSubCategory() {
        return "Cluster";
    }
}
