package ca.kijiji.replyts.newreplierfilter;

import ca.kijiji.replyts.ActivableFilter;
import ca.kijiji.replyts.Activation;
import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class NewReplierFilter extends ActivableFilter {

    private static final Logger LOG = LoggerFactory.getLogger(NewReplierFilter.class);

    private static final String IS_NEW_KEY = "is-new";

    private final int newReplierScore;
    private final TnsApiClient tnsApiClient;

    NewReplierFilter(int newReplierScore, Activation activation, TnsApiClient tnsApiClient) {
        super(activation);
        this.newReplierScore = newReplierScore;
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    protected List<FilterFeedback> doFilter(MessageProcessingContext context) {

        String from = context.getMail().getFrom();

        boolean isNew = false;
        try {
            isNew = checkIfEmailNewInLeGrid(from);
        } catch (Exception e) {
            LOG.warn("Exception caught when calling grid. Assuming replier not new.", e);
        }

        ImmutableList.Builder<FilterFeedback> feedbacks = ImmutableList.builder();

        if (isNew) {
            feedbacks.add(new FilterFeedback("email is new", "Replier email is new",
                    newReplierScore, FilterResultState.OK));
        }

        return feedbacks.build();
    }

    private boolean checkIfEmailNewInLeGrid(String from) {
        Map<String, Boolean> result = tnsApiClient.getJsonAsMap("/replier/email/" + from + "/is-new");

        boolean isNew = result.get(IS_NEW_KEY);
        LOG.trace("Is user {} new? {}", from, isNew);
        return isNew;
    }

}
