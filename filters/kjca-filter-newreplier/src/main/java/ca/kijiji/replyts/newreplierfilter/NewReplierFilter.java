package ca.kijiji.replyts.newreplierfilter;

import ca.kijiji.replyts.ActivableFilter;
import ca.kijiji.replyts.Activation;
import ca.kijiji.replyts.TnsApiClient;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class NewReplierFilter extends ActivableFilter {

    private static final Logger LOG = LoggerFactory.getLogger(NewReplierFilter.class);

    static final String IS_NEW_KEY = "is-new";
    static final String UI_HINT = "email is new";
    static final String DESCRIPTION = "Replier email is new";

    private final int newReplierScore;
    private final TnsApiClient tnsApiClient;

    NewReplierFilter(int newReplierScore, Activation activation, TnsApiClient tnsApiClient) {
        super(activation);
        this.newReplierScore = newReplierScore;
        this.tnsApiClient = tnsApiClient;
    }

    @Override
    protected List<FilterFeedback> doFilter(MessageProcessingContext context) {
        String from = context.getMail().get().getFrom();
        if (StringUtils.isBlank(from)) {
            LOG.warn("No e-mail provided for context {}", context.toString());
        } else if (StringUtils.isNotBlank(from) && checkIfEmailNewInLeGrid(from)) {
            return ImmutableList.of(new FilterFeedback("email is new", "Replier email is new", newReplierScore, FilterResultState.OK));
        }
        return ImmutableList.of();
    }

    private boolean checkIfEmailNewInLeGrid(String from) {
        Map<String, Boolean> result = tnsApiClient.getJsonAsMap("/replier/email/" + from + "/is-new");
        if (result.get(IS_NEW_KEY) == null) {
            LOG.warn("No proper result from TnsApi for user {}, assuming he/she is not new", from);
            return false;
        } else {
            boolean isNew = result.get(IS_NEW_KEY);
            LOG.debug("Is user {} new? {}", from, isNew);
            return isNew;
        }
    }
}
