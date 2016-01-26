package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.runtime.DateSliceIterator.IterationDirection;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor.ErrorHandlingPolicy;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class IndexingModeTest {

    @Test
    public void errorPolicyShouldFailFastOnDeltaIndexing() {
        assertThat(IndexingMode.DELTA.errorHandlingPolicy()).isEqualTo(ErrorHandlingPolicy.FAIL_FAST_ON_ERRORS);
    }

    @Test
    public void errorPolicyShouldSkipErrorsOnFullIndexing() {
        assertThat(IndexingMode.FULL.errorHandlingPolicy()).isEqualTo(ErrorHandlingPolicy.SKIP_ERRORS);
    }

    @Test
    public void iterationDirectionShouldPresentToPastOnFullIndexing() {
        assertThat(IndexingMode.FULL.indexingDirection()).isEqualTo(IterationDirection.PRESENT_TO_PAST);
    }

    @Test
    public void iterationDirectionShouldPastToPresentOnDeltaIndexing() {
        assertThat(IndexingMode.DELTA.indexingDirection()).isEqualTo(IterationDirection.PAST_TO_PRESENT);
    }
}
