package com.ecg.replyts.core.api.model.conversation;

/**
 * Result states for {@link Message} as decided by either a filter or a human.
 */
public enum FilterResultState {

    /**
     * Message was considered fine and therefore sent.
     */
    OK {
        @Override
        public boolean isTransitionByFiltersAllowedTo(FilterResultState otherState) {
            return true;
        }
    },
    /**
     * Message is currently waiting for CS staff to decide on the final state.
     */
    HELD {
        @Override
        public boolean isTransitionByFiltersAllowedTo(FilterResultState otherState) {
            switch (otherState) {
                case ACCEPT_AND_TERMINATE:
                case HELD:
                case DROPPED:
                    return true;
            }
            return false;
        }
    },
    /**
     * Message was considered trustworthy and therefore sent.
     */
    ACCEPT_AND_TERMINATE {
        @Override
        public boolean isTransitionByFiltersAllowedTo(FilterResultState otherState) {
            return false;
        }

    },
    /**
     * Message was considered harmful and therefore not delivered.
     */
    DROPPED {
        @Override
        public boolean isTransitionByFiltersAllowedTo(FilterResultState otherState) {
            switch (otherState) {
                case ACCEPT_AND_TERMINATE:
                    return true;
            }
            return false;
        }


    };

    public abstract boolean isTransitionByFiltersAllowedTo(FilterResultState otherState);
}
