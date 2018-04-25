package com.ecg.messagecenter.gtuk.cleanup;

public interface CleanupAdvice {
    boolean isLineQuoted(int index);

    boolean isLineCleaned(int index);
}