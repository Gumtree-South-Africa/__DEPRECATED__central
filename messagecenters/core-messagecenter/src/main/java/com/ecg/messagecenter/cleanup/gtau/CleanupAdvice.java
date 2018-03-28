package com.ecg.messagecenter.cleanup.gtau;

public interface CleanupAdvice {
    boolean isLineQuoted(int index);
    boolean isLineCleaned(int index);
}