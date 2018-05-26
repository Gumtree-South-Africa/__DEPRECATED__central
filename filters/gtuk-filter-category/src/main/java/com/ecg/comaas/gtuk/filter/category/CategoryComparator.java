package com.ecg.comaas.gtuk.filter.category;

import java.util.Comparator;

import static com.ecg.comaas.gtuk.filter.category.CategoryComparator.WellKnown.*;

public class CategoryComparator implements Comparator<Category> {

    private final String OTHER = "OTHER";

    public int compare(Category category1, Category category2) {
        if (category1 == null || category2 == null) {
            throw new IllegalStateException("Category cannot be null!");
        }

        //L1 have a specific Order
        if (MOTORS.getSeoName().equals(category1.getSeoName())) {
            return -8;
        } else if (MOTORS.getSeoName().equals(category2.getSeoName())) {
            return 8;
        }

        if (FOR_SALE.getSeoName().equals(category1.getSeoName())) {
            return -7;
        } else if (FOR_SALE.getSeoName().equals(category2.getSeoName())) {
            return 7;
        }

        if (FLATS_AND_HOUSES.getSeoName().equals(category1.getSeoName())) {
            return -6;
        } else if (FLATS_AND_HOUSES.getSeoName().equals(category2.getSeoName())) {
            return 6;
        }

        if (JOBS.getSeoName().equals(category1.getSeoName())) {
            return -5;
        } else if (JOBS.getSeoName().equals(category2.getSeoName())) {
            return 5;
        }

        if (SERVICES.getSeoName().equals(category1.getSeoName())) {
            return -4;
        } else if (SERVICES.getSeoName().equals(category2.getSeoName())) {
            return 4;
        }

        if (COMMUNITY.getSeoName().equals(category1.getSeoName())) {
            return -3;
        } else if (COMMUNITY.getSeoName().equals(category2.getSeoName())) {
            return 3;
        }

        if (PETS.getSeoName().equals(category1.getSeoName())) {
            return -2;
        } else if (PETS.getSeoName().equals(category2.getSeoName())) {
            return 2;
        }

        //Other is Always to be put at the Bottom
        if (category1.getSeoName().toUpperCase().contains(OTHER)) {
            return 1;
        } else if (category2.getSeoName().toUpperCase().contains(OTHER)) {
            return -1;
        }

        return category1.getName().toUpperCase().compareTo(category2.getName().toUpperCase());
    }

    static enum WellKnown {
        FLATS_AND_HOUSES("flats-houses"),
        MOTORS("cars-vans-motorbikes"),
        JOBS("jobs"),
        PETS("pets"),
        SERVICES("business-services"),
        COMMUNITY("community"),
        FOR_SALE("for-sale");

        private final String seoName;

        WellKnown(String seoName) {
            this.seoName = seoName;
        }

        public String getSeoName() {
            return seoName;
        }
    }
}
