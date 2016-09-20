package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.runtime.DateSliceIterator.IterationDirection;
import com.ecg.replyts.core.runtime.workers.BlockingBatchExecutor.ErrorHandlingPolicy;

/**
 * Configuration of the indexing mode.
 */
interface IndexingModeConfiguration {
    ErrorHandlingPolicy errorHandlingPolicy();
    IterationDirection indexingDirection();
}

/**
 * Specify indexing mode and related configurations for that .
 */
public enum IndexingMode implements IndexingModeConfiguration {

    MIGRATION {

        @Override
        public ErrorHandlingPolicy errorHandlingPolicy() {
            return ErrorHandlingPolicy.SKIP_ERRORS;
        }

        @Override
        public IterationDirection indexingDirection() {
            return IterationDirection.PRESENT_TO_PAST;
        }
    },
    FULL {

        @Override
        public ErrorHandlingPolicy errorHandlingPolicy() {
            return ErrorHandlingPolicy.SKIP_ERRORS;
        }

        @Override
        public IterationDirection indexingDirection() {
            return IterationDirection.PRESENT_TO_PAST;
        }
    },
    DELTA {

        @Override
        public ErrorHandlingPolicy errorHandlingPolicy() {
            return ErrorHandlingPolicy.FAIL_FAST_ON_ERRORS;
        }

        @Override
        public IterationDirection indexingDirection() {
            return IterationDirection.PAST_TO_PRESENT;
        }
    };
}


