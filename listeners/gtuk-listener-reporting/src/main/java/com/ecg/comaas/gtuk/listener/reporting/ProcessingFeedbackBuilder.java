package com.ecg.comaas.gtuk.listener.reporting;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ImmutableProcessingFeedback;
import com.ecg.replyts.core.api.model.conversation.ProcessingFeedback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ProcessingFeedbackBuilder {
        private String filterName;
        private String filterInstance;
        private String uiHint;
        private Integer score;
        private FilterResultState resultState;
        private boolean evaluation;

        public ProcessingFeedbackBuilder filterName(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public ProcessingFeedbackBuilder filterInstance(String filterInstance) {
            this.filterInstance = filterInstance;
            return this;
        }

        public ProcessingFeedbackBuilder uiHint(String uiHint) {
            this.uiHint = uiHint;
            return this;
        }

        public ProcessingFeedbackBuilder score(Integer score) {
            this.score = score;
            return this;
        }

        public ProcessingFeedbackBuilder resultState(FilterResultState resultState) {
            this.resultState = resultState;
            return this;
        }

        public ProcessingFeedbackBuilder evaluation(boolean evaluation) {
            this.evaluation = evaluation;
            return this;
        }

        public ProcessingFeedback createBogusProcessingFeedback() {
            return new ImmutableProcessingFeedback(
                    "com.ecg.replyts.app.filterchain.FilterChain",
                    "default",
                    "BLOCKED",
                    "FilterChain ended up in result state DROPPED",
                    null,
                    null,
                    false
            );
        }

        public ProcessingFeedback createProcessingFeedback() {
            return new ImmutableProcessingFeedback(
                    "com.gumtree.replyts2.filter.GumtreeBaseFilterFactory",
                    "gumtreeRts2FilterConfig",
                    uiHint,
                    createFilterJsonString(filterName, filterInstance, "n/a", "some description"),
                    score,
                    resultState,
                    evaluation
            );
        }

        private String createFilterJsonString(String name, String instance, String version, String description) {
            ObjectMapper objectMapper = new ObjectMapper();

            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("filterName", name);
            objectNode.put("filterInstance", instance);
            objectNode.put("filterVersion", version);
            objectNode.put("description", description);

            try {
                return objectMapper.writeValueAsString(objectNode);
            } catch (JsonProcessingException e) {
                return "{}";
            }
        }
    }