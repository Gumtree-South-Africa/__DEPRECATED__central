package com.ecg.comaas.mde.postprocessor.demandreporting.domain;

public interface Event {
    String getEventType();
    CommonEventData getCommonEventData();
}