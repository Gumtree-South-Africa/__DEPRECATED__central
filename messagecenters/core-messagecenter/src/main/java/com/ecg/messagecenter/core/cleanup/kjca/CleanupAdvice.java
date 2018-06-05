package com.ecg.messagecenter.core.cleanup.kjca;

/**
 * Created by pragone on 19/04/15.
 */
public interface CleanupAdvice {
    boolean isLineQuoted(int index);
    boolean isLineCleaned(int index);
}
