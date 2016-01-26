package com.ecg.replyts.core.api.model.conversation;

import com.ecg.replyts.core.api.util.Pairwise;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

public class ImmutableProcessingFeedback implements ProcessingFeedback {

    private final String filterName;
    private final String filterInstance;
    private final String uiHint;
    private final String description;
    private final Integer score;
    private final FilterResultState resultState;
    private final boolean evaluation;

    @JsonCreator
    public ImmutableProcessingFeedback(
            @JsonProperty("filterName") String filterName,
            @JsonProperty("filterInstance") String filterInstance,
            @JsonProperty("uiHint") String uiHint,
            @JsonProperty("description") String description,
            @JsonProperty("score") Integer score,
            @JsonProperty("resultState") FilterResultState resultState,
            @JsonProperty("evaluation") boolean evaluation) {
        notEmpty(filterName);
        notEmpty(filterInstance);
        this.filterName = filterName;
        this.filterInstance = filterInstance;
        this.uiHint = uiHint;
        this.description = description;
        this.score = score;
        this.resultState = resultState;
        this.evaluation = evaluation;
    }

    public String getFilterName() {
        return filterName;
    }

    public String getFilterInstance() {
        return filterInstance;
    }

    public String getUiHint() {
        return uiHint;
    }

    public String getDescription() {
        return description;
    }

    public Integer getScore() {
        return score;
    }

    public FilterResultState getResultState() {
        return resultState;
    }

    public boolean isEvaluation() {
        return evaluation;
    }

    private static void notEmpty(String filterName) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(filterName));
    }

    @Override
    public String toString() {
        return "ImmutableProcessingFeedback{" +
                "filterName='" + filterName + '\'' +
                ", filterInstance='" + filterInstance + '\'' +
                ", uiHint='" + uiHint + '\'' +
                ", description='" + description + '\'' +
                ", score=" + score +
                ", resultState=" + resultState +
                ", evaluation=" + evaluation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImmutableProcessingFeedback that = (ImmutableProcessingFeedback) o;

        return Pairwise.pairsAreEqual(
                evaluation, that.evaluation,
                description, that.description,
                filterInstance, that.filterInstance,
                filterName, that.filterName,
                resultState, that.resultState,
                score, that.score,
                uiHint, that.uiHint
        );
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(evaluation, description, filterInstance, filterName, resultState, score, uiHint);
    }
}
