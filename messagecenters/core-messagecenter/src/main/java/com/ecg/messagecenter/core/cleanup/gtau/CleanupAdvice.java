package com.ecg.messagecenter.core.cleanup.gtau;

public interface CleanupAdvice {
    boolean isLineQuoted(int index);
    boolean isLineCleaned(int index);
}