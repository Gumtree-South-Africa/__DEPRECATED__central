package com.ecg.comaas.ebayk.filter.phonenumber;

import com.google.common.collect.ImmutableList;

import java.util.List;

public class NumberStream {

    private final List<String> items;

    public NumberStream(List<String> items) {
        this.items = ImmutableList.copyOf(items);
    }

    List<String> getItems() {
        return items;
    }

    public boolean contains(String accountNumber) {
        return items.stream()
                .filter(item -> item.contains(accountNumber))
                .findFirst()
                .isPresent();
    }

}
