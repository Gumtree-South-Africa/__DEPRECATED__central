package com.ecg.comaas.mp.filter.bankaccount;

public class BankAccountFilterUtil {

    public static <T> T coalesce(T... ts) {
        for (T t : ts) {
            if (t != null) return t;
        }
        return null;
    }
}
