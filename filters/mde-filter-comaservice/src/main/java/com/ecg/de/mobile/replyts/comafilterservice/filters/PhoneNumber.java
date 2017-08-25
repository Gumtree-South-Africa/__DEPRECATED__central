package com.ecg.de.mobile.replyts.comafilterservice.filters;

public class PhoneNumber {
    private final int countryCode;

    private final String displayNumber;

    public PhoneNumber(int countryCode, String displayNumber){
        this.countryCode = countryCode;
        this.displayNumber = displayNumber;
    }

    public int getCountryCode() {
        return countryCode;
    }

    public String getDisplayNumber() {
        return displayNumber;
    }

    @Override
    public String toString() {
        return "PhoneNumber [countryCode=" + countryCode + ", displayNumber="
                + displayNumber + "]";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhoneNumber that = (PhoneNumber) o;

        if (countryCode != that.countryCode) return false;
        if (displayNumber != null ? !displayNumber.equals(that.displayNumber) : that.displayNumber != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = countryCode;
        result = 31 * result + (displayNumber != null ? displayNumber.hashCode() : 0);
        return result;
    }
}