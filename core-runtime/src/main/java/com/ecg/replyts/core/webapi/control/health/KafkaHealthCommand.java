package com.ecg.replyts.core.webapi.control.health;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;
import java.util.Map;

public class KafkaHealthCommand extends AbstractHealthCommand {

    private final Consumer<?, ?> kafkaConsumer;

    KafkaHealthCommand(Consumer<?, ?> kafkaConsumer) {
        this.kafkaConsumer = kafkaConsumer;
    }

    @Override
    public ObjectNode execute() {
        try {
            Map<String, List<PartitionInfo>> kafkaTopics = kafkaConsumer.listTopics();

            return JsonObjects.builder()
                    .attr(STATUS, Status.UP.name())
                    .attr("topics", JsonObjects.newJsonArray(kafkaTopics.keySet()))
                    .build();

        } catch (Exception ex) {
            return status(Status.DOWN, ex.getMessage());
        }
    }

    @Override
    public String name() {
        return "kafka";
    }
}
