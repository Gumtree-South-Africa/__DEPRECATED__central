package com.ecg.replyts.core.api.model.conversation;

/**
 * Result states for {@link Message} as decided by either a filter or a human.
 * <p>
 * OK means pass.
 * HELD means the message will be held for review.
 * DROPPED is the "heaviest" filter, and stops delivery of the message.
 * <p>
 * ACCEPT_AND_TERMINATE should not be used as a final FilterResult. This is just a flag or internal state to stop further
 * processing of filters and return the "heaviest" result up to, and not including, the ACCEPT_AND_TERMINATE state.
 * <p>
 * E.g. for 3 configured filters:
 * results(OK, OK, OK) -> OK
 * results(DROPPED, OK, OK) -> DROPPED (but will process other filters)
 * results(DROPPED, ACCEPT_AND_TERMINATE) -> DROPPED (third filter is not processed)
 */
public enum FilterResultState {
    /**
     * Message was considered fine
     */
    OK {
        @Override
        public boolean hasLowerPriorityThan(FilterResultState otherState) {
            checkForCorrectOtherState(otherState);
            return otherState.equals(HELD) || otherState.equals(DROPPED);
        }
    },
    /**
     * Message is currently waiting for CS staff to decide on the final state
     */
    HELD {
        @Override
        public boolean hasLowerPriorityThan(FilterResultState otherState) {
            checkForCorrectOtherState(otherState);
            return otherState.equals(DROPPED);
        }
    },
    /**
     * Message was considered harmful and therefore not delivered
     */
    DROPPED {
        @Override
        public boolean hasLowerPriorityThan(FilterResultState otherState) {
            checkForCorrectOtherState(otherState);
            return false;
        }
    },
    /**
     * Stop processing remaining filters and return results up to and excluding ACCEPT_AND_TERMINATE
     */
    ACCEPT_AND_TERMINATE {
        @Override
        public boolean hasLowerPriorityThan(FilterResultState otherState) {
            throw new IllegalStateException("Cannot transition out of ACCEPT_AND_TERMINATE, should stop filter chain instead");
        }
    };

    public void checkForCorrectOtherState(FilterResultState otherState) {
        if (otherState.equals(ACCEPT_AND_TERMINATE)) {
            throw new IllegalStateException("Cannot transition to ACCEPT_AND_TERMINATE, should stop filter chain instead");
        }
    }

    public abstract boolean hasLowerPriorityThan(FilterResultState otherState);
}
