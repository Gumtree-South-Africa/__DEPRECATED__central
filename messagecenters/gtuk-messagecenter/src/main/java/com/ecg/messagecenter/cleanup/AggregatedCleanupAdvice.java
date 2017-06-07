package com.ecg.messagecenter.cleanup;

import java.util.ArrayList;
import java.util.List;

public class AggregatedCleanupAdvice implements CleanupAdvice {
    private List<CleanupAdvice> cleanupAdviceList = new ArrayList<>();

    public void addAdvice(CleanupAdvice cleanupAdvice) {
        cleanupAdviceList.add(cleanupAdvice);
    }

    @Override
    public boolean isLineQuoted(int index) {
        for (CleanupAdvice cleanupAdvice : cleanupAdviceList) {
            if (cleanupAdvice.isLineQuoted(index)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isLineCleaned(int index) {
        for (CleanupAdvice cleanupAdvice : cleanupAdviceList) {
            if (cleanupAdvice.isLineCleaned(index)) {
                return true;
            }
        }
        return false;
    }
}