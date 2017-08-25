package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.cap.ConflictResolver;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;

public class RiakSimplePostBoxConflictResolver implements ConflictResolver<PostBox> {
    private static final Logger LOG = LoggerFactory.getLogger(RiakSimplePostBoxConflictResolver.class);

    private static final Counter POSTBOX_MERGE_COUNTER = TimingReports.newCounter("riak-postbox-merges");
    private static final Histogram POSTBOX_SIBLING_COUNT_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sibling-counts");

    @Autowired
    private RiakSimplePostBoxMerger merger;

    public PostBox resolve(Collection<PostBox> postBoxesToResolve) {
        if (postBoxesToResolve.isEmpty()) {
            return null;
        }

        POSTBOX_SIBLING_COUNT_HISTOGRAM.update(postBoxesToResolve.size() - 1);

        // by definition size()==1 is NO conflict (sibling == 1 is reflexive)
        if (postBoxesToResolve.size() == 1) {
            return postBoxesToResolve.iterator().next();
        }

        POSTBOX_MERGE_COUNTER.inc();

        if (postBoxesToResolve.size() > 5) {
            LOG.info("High number '{}' of siblings found for postbox #{}",
                    postBoxesToResolve.size(),
                    postBoxesToResolve.iterator().next().getEmail());
        }

        return merger.merge(postBoxesToResolve);
    }
}
