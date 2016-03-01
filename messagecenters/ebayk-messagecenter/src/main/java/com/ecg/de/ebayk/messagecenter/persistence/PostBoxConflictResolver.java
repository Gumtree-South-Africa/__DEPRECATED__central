package com.ecg.de.ebayk.messagecenter.persistence;

import com.basho.riak.client.cap.ConflictResolver;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.ecg.replyts.core.runtime.TimingReports;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static java.lang.String.format;

/**
 * User: maldana
 * Date: 23.10.13
 * Time: 16:26
 *
 * @author maldana@ebay.de
 */
class PostBoxConflictResolver implements ConflictResolver<PostBox> {

    private static final Logger LOG = LoggerFactory.getLogger(PostBoxConflictResolver.class);

    private static final Counter POSTBOX_MERGE_COUNTER = TimingReports.newCounter("riak-postbox-merges");
    private static final Histogram POSTBOX_SIBLING_COUNT_HISTOGRAM = TimingReports.newHistogram("riak-postbox-sibling-counts");

    private final PostBoxesMerger merger =new PostBoxesMerger () ;


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
            LOG.info(format("High number '%d' of siblings found for postbox #%s",
                    postBoxesToResolve.size(),
                    postBoxesToResolve.iterator().next().getEmail()));
        }

        return merger.merge(postBoxesToResolve);
    }

}
