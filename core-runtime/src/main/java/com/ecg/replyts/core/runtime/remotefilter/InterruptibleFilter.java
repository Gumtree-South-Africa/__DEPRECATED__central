package com.ecg.replyts.core.runtime.remotefilter;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;

/**
 * Filter that respects interrupts; a lot of filters are not guaranteed to do this, but these filters
 * can be assumed to be 'safe'.
 * <p>
 * When interrupted, they throw some RuntimeException for which {@link InterruptibleFilter#isFilterInterruptException} is true.
 */
public interface InterruptibleFilter extends Filter {

    /**
     * create a runtimeException that satisfies {@link #isFilterInterruptException}.
     */
    static RuntimeException createInterruptedException() {
        // do not depend on this implementation detail
        return new RuntimeException(new InterruptedException());
    }

    /**
     * if this method returns true, the Throwable indicates an {@link InterruptibleFilter} was interrupted.
     */
    static boolean isFilterInterruptException(Throwable t) {
        return (t.getCause() instanceof InterruptedException);
    }
}
