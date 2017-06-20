package com.ecg.messagebox.model;

import java.util.Objects;

public class AggregatedResponseData {

    private int speed;
    private int rate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AggregatedResponseData that = (AggregatedResponseData) o;
        return speed == that.speed && rate == that.rate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(speed, rate);
    }

    public AggregatedResponseData(int speed, int rate) {
        this.speed = speed;
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "AggregatedResponseData{" +
                "speed=" + speed +
                ", rate=" + rate +
                '}';
    }
}
