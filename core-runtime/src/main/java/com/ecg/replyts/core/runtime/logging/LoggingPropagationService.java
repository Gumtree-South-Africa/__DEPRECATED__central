package com.ecg.replyts.core.runtime.logging;

import com.ecg.replyts.core.LoggingService;
import com.google.gson.Gson;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.Map;

@Component
public class LoggingPropagationService {
    @Autowired
    private HazelcastInstance hazelcastInstance;

    @Autowired
    private LoggingService loggingService;

    private ITopic<String> hazelcastTopic;

    @PostConstruct
    private void initialize() {
        hazelcastTopic = hazelcastInstance.getTopic("logging_levels");
        hazelcastTopic.addMessageListener(this::process);
    }

    private void process(Message<String> message) {
        LogLevelCommand command = new Gson().fromJson(message.getMessageObject(), LogLevelCommand.class);

        switch(command.getType()) {
            case REPLACE_ALL:
                loggingService.replaceAll(command.getLevels());

                break;
            case UPSERT_AND_SET:
                if (command.getLevels().size() == 1) {
                    Map.Entry<String, String> entry = command.getLevels().entrySet().iterator().next();

                    loggingService.upsertAndSet(entry.getKey(), entry.getValue());
                }

                break;
            case INITIALIZE_TO_PROPERTIES:
                loggingService.initializeToProperties();

                break;
        }
    }

    public void propagateReplaceAll(Map<String, String> levels) {
        hazelcastTopic.publish(new Gson().toJson(new LogLevelCommand(UpdateType.REPLACE_ALL, levels)));
    }

    public void propagateUpsertAndSet(String logPackage, String level) {
        hazelcastTopic.publish(new Gson().toJson(new LogLevelCommand(UpdateType.UPSERT_AND_SET, Collections.singletonMap(logPackage, level))));
    }

    public void propagateInitializeToProperties() {
        hazelcastTopic.publish(new Gson().toJson(new LogLevelCommand(UpdateType.INITIALIZE_TO_PROPERTIES, null)));
    }

    enum UpdateType {
        REPLACE_ALL,
        UPSERT_AND_SET,
        INITIALIZE_TO_PROPERTIES;
    }

    class LogLevelCommand {
        private UpdateType type;

        private Map<String, String> levels;

        public LogLevelCommand(UpdateType type, Map<String, String> levels) {
            this.type = type;
            this.levels = levels;
        }

        public UpdateType getType() {
            return type;
        }

        public Map<String, String> getLevels() {
            return levels;
        }
    }
}