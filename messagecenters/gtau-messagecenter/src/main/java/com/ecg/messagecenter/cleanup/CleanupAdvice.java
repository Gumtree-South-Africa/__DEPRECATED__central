package com.ecg.messagecenter.cleanup;

public interface CleanupAdvice {
    boolean isLineQuoted(int index);
    boolean isLineCleaned(int index);
}