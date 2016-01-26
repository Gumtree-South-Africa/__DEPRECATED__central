package com.ecg.replyts.core.runtime.model.conversation;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.ImmutableProcessingFeedback;

public final class ProcessingFeedbackBuilder {

    private String filterName;
    private String filterInstance;
    private String uiHint;
    private String description;
    private Integer score;
    private FilterResultState resultState;
    private boolean evaluation;

    private ProcessingFeedbackBuilder() {
    }

    public static ProcessingFeedbackBuilder aProcessingFeedback() {
        return new ProcessingFeedbackBuilder();
    }

    public ProcessingFeedbackBuilder withFilterName(String filterName) {
        this.filterName = filterName;
        return this;
    }

    public ProcessingFeedbackBuilder withFilterInstance(String filterInstance) {
        this.filterInstance = filterInstance;
        return this;
    }

    public ProcessingFeedbackBuilder withUiHint(String uiHint) {
        this.uiHint = uiHint;
        return this;
    }

    public ProcessingFeedbackBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ProcessingFeedbackBuilder withScore(Integer score) {
        this.score = score;
        return this;
    }

    public ProcessingFeedbackBuilder withResultState(FilterResultState resultState) {
        this.resultState = resultState;
        return this;
    }

    public ProcessingFeedbackBuilder withEvaluation(boolean evaluation) {
        this.evaluation = evaluation;
        return this;
    }

    public ImmutableProcessingFeedback build() {
        return new ImmutableProcessingFeedback(filterName, filterInstance, uiHint, description, score, resultState, evaluation);
    }
}
