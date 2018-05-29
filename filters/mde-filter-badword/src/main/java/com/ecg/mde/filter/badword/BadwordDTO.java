package com.ecg.mde.filter.badword;

import java.util.Date;

public class BadwordDTO {

    private final String id;

    private final String creator;

    private final String term;

    private final String stemmed;

    private final Date creationTime;

    private final BadwordType type;

    private BadwordDTO(Builder builder) {
        this.id = builder.id;
        creator = builder.creator;
        term = builder.term;
        stemmed = builder.stemmed;
        creationTime = builder.creationTime;
        type = builder.type;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(BadwordDTO copy) {
        Builder builder = new Builder();
        builder.creator = copy.creator;
        builder.term = copy.term;
        builder.stemmed = copy.stemmed;
        builder.creationTime = copy.creationTime;
        builder.type = copy.type;
        return builder;
    }

    public String getId() {
        return id;
    }

    public String getCreator() {
        return creator;
    }

    public String getTerm() {
        return term;
    }

    public String getStemmed() {
        return stemmed;
    }

    public Date getCreationTime() {
        return creationTime;
    }

    public BadwordType getType() {
        return type;
    }

    public static final class Builder {

        private String id;

        private String creator;

        private String term;

        private String stemmed;

        private Date creationTime;

        private BadwordType type;

        private Builder() {
        }

        public Builder id(String val) {
            id = val;
            return this;
        }

        public Builder creator(String val) {
            creator = val;
            return this;
        }

        public Builder term(String val) {
            term = val;
            return this;
        }

        public Builder stemmed(String val) {
            stemmed = val;
            return this;
        }

        public Builder creationTime(Date val) {
            creationTime = val;
            return this;
        }

        public Builder type(BadwordType val) {
            type = val;
            return this;
        }

        public BadwordDTO build() {
            return new BadwordDTO(this);
        }
    }

    @Override
    public String toString() {
        return "BadwordDTO{" +
                "id='" + id + '\'' +
                ", creator='" + creator + '\'' +
                ", term='" + term + '\'' +
                ", stemmed='" + stemmed + '\'' +
                ", creationTime=" + creationTime +
                ", type=" + type +
                '}';
    }
}