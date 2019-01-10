package com.ecg.replyts.core.api.pluginconfiguration.filter;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.google.common.base.Objects;

/**
 * Output of a filter hit that describes why the filter fired on a particular message to the system and to CS agents.
 * Filter hits contain an ui hint (something parseable, that should be groupable), a description (something, CS agents can understand)
 * a score and a result state. Scores are used by result inspectors, result states can be used to send messages into held or blocked directly.
 *
 * @author mhuttar
 */
public class FilterFeedback {

    private final String uiHint;
    private final String description;
    private final Integer score;
    private final FilterResultState resultState;

    public FilterFeedback(String uiHint, String description, Integer score, FilterResultState resultState) {
        this.uiHint = uiHint;
        this.description = description;
        this.score = score;
        this.resultState = resultState;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FilterFeedback that = (FilterFeedback) o;

        return Objects.equal(this.description, that.description) &&
                Objects.equal(this.resultState, that.resultState) &&
                Objects.equal(this.score, that.score) &&
                Objects.equal(this.uiHint, that.uiHint);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(description, resultState, score, uiHint);
    }

    @Override
    public String toString() {
        return "FilterFeedback{" +
                "uiHint='" + uiHint + '\'' +
                ", description='" + description + '\'' +
                ", score=" + score +
                ", resultState=" + resultState +
                '}';
    }
}
