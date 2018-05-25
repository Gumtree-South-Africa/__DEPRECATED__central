package com.ecg.comaas.gtuk.listener.statsnotifier.event;

import com.ecg.comaas.gtuk.listener.statsnotifier.AnalyticsEvent;

import java.util.HashMap;
import java.util.Optional;

public class GAEvent implements AnalyticsEvent {

    protected String clientId;
    protected String eventCategory;
    protected HashMap<Integer, String> customDimension = new HashMap<>();

    protected GAEvent(GAEventBuilder builder) {
        this.clientId = builder.clientId;
        this.eventCategory = builder.eventCategory;
        this.customDimension = builder.customDimension;
    }

    public Optional<String> getClientId() {
        return Optional.ofNullable(clientId);
    }

    public String getEventCategory() {
        return eventCategory;
    }

    public HashMap<Integer, String> getCustomDimension() {
        return customDimension;
    }

    public String getEventAction() {
        return this.getClass().getSimpleName();
    }

    public abstract static class GAEventBuilder<T extends GAEvent> {
        protected String clientId;
        protected String eventCategory;
        protected HashMap<Integer, String> customDimension = new HashMap<>();

        public GAEventBuilder withEventCategory(String eventCategory) {
            this.eventCategory = eventCategory;
            return this;
        }

        public GAEventBuilder withCustomDimension(Integer dimension, String value) {
            customDimension.put(dimension, value);
            return this;
        }

        public GAEventBuilder withClientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public abstract T build();
    }

    @Override
    public boolean equals(Object o) {
        if(this == o){
            return true;
        }
        if(!(o instanceof GAEvent)){
            return false;
        }

        GAEvent gaEvent = (GAEvent) o;

        if(clientId != null ? !clientId.equals(gaEvent.clientId) : gaEvent.clientId != null){
            return false;
        }
        if(customDimension != null ? !customDimension.equals(gaEvent.customDimension) : gaEvent.customDimension != null){
            return false;
        }
        if(eventCategory != null ? !eventCategory.equals(gaEvent.eventCategory) : gaEvent.eventCategory != null){
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientId != null ? clientId.hashCode() : 0;
        result = 31 * result + (eventCategory != null ? eventCategory.hashCode() : 0);
        result = 31 * result + (customDimension != null ? customDimension.hashCode() : 0);
        return result;
    }
}
