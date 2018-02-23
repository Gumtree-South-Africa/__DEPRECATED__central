package com.ecg.messagebox.resources.responses;

import io.swagger.annotations.ApiModelProperty;

public class AggregatedResponseDataResponse {

    @ApiModelProperty(required = true)
    private final int speed;
    @ApiModelProperty(required = true)
    private final int rate;

    public AggregatedResponseDataResponse(int speed, int rate) {
        this.speed = speed;
        this.rate = rate;
    }

    public int getSpeed() {
        return speed;
    }

    public int getRate() {
        return rate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregatedResponseDataResponse that = (AggregatedResponseDataResponse) o;
        return speed == that.speed &&
                rate == that.rate;
    }

    @Override
    public int hashCode() {
        return com.google.common.base.Objects.hashCode(speed, rate);
    }
}
