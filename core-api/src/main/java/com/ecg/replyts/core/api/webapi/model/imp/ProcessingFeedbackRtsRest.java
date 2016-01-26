package com.ecg.replyts.core.api.webapi.model.imp;

import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.webapi.model.ProcessingFeedbackRts;

import java.io.Serializable;

public class ProcessingFeedbackRtsRest implements ProcessingFeedbackRts, Serializable {

    private Integer score;

    private Boolean evaluation;

    private FilterResultState state;

    private String filterName;

    private String filterInstance;

    private String description;

    private String uiHint;

    public ProcessingFeedbackRtsRest() {

    }

    @Override
    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    @Override
    public Boolean getEvaluation() {
        return evaluation;
    }

    public void setEvaluation(Boolean evaluation) {
        this.evaluation = evaluation;
    }

    @Override
    public FilterResultState getState() {
        return state;
    }

    public void setState(FilterResultState state) {
        this.state = state;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    @Override
    public String getFilterInstance() {
        return filterInstance;
    }

    public void setFilterInstance(String filterInstance) {
        this.filterInstance = filterInstance;
    }

    @Override
    public String getUiHint() {
        return uiHint;
    }

    public void setUiHint(String uiHint) {
        this.uiHint = uiHint;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
