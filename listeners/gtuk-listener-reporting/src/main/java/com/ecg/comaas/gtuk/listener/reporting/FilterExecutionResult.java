package com.ecg.comaas.gtuk.listener.reporting;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilterExecutionResult {

    @JsonProperty("filter_name")
    String filterName;

    @JsonProperty("filter_instance")
    String filterInstance;

    @JsonProperty("ui_hint")
    String uiHint;

    @JsonProperty("score")
    Integer score;

    @JsonProperty("result_state")
    String resultState;

    @JsonProperty("evaluation")
    boolean evaluation;

    public FilterExecutionResult(
            String filterName,
            String filterInstance,
            String uiHint,
            Integer score,
            String resultState,
            boolean evaluation) {

        this.filterName = filterName;
        this.filterInstance = filterInstance;
        this.uiHint = uiHint;
        this.score = score;
        this.resultState = resultState;
        this.evaluation = evaluation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }

        FilterExecutionResult that = (FilterExecutionResult) o;

        if (evaluation != that.evaluation){
            return false;
        }
        if (!filterName.equals(that.filterName)){
            return false;
        }
        if (!filterInstance.equals(that.filterInstance)){
            return false;
        }
        if (!uiHint.equals(that.uiHint)){
            return false;
        }
        if (score != null ? !score.equals(that.score) : that.score != null){
            return false;
        }
        return !(resultState != null ? !resultState.equals(that.resultState) : that.resultState != null);
    }

    @Override
    public int hashCode() {
        int result = filterName.hashCode();
        result = 31 * result + filterInstance.hashCode();
        result = 31 * result + uiHint.hashCode();
        result = 31 * result + (score != null ? score.hashCode() : 0);
        result = 31 * result + (resultState != null ? resultState.hashCode() : 0);
        result = 31 * result + (evaluation ? 1 : 0);
        return result;
    }

    public static class Builder {
        private String filterName;
        private String filterInstance;
        private String uiHint;
        private Integer score;
        private String resultState;
        private boolean evaluation;

        public Builder filterName(String filterName) {
            this.filterName = filterName;
            return this;
        }

        public Builder filterInstance(String filterInstance) {
            this.filterInstance = filterInstance;
            return this;
        }

        public Builder uiHint(String uiHint) {
            this.uiHint = uiHint;
            return this;
        }

        public Builder score(Integer score) {
            this.score = score;
            return this;
        }

        public Builder resultState(String resultState) {
            this.resultState = resultState;
            return this;
        }

        public Builder evaluation(boolean evaluation) {
            this.evaluation = evaluation;
            return this;
        }

        public FilterExecutionResult createFilterExecutionResult() {
            return new FilterExecutionResult(
                    filterName,
                    filterInstance,
                    uiHint,
                    score,
                    resultState,
                    evaluation);
        }
    }
}
