package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.IndexEntry;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.cap.ConflictResolver;
import com.basho.riak.client.cap.DefaultRetrier;
import com.basho.riak.client.cap.Quora;
import com.basho.riak.client.convert.Converter;
import com.basho.riak.client.query.StreamingOperation;
import com.basho.riak.client.query.indexes.IntIndex;
import com.codahale.metrics.Timer;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DefaultRiakSimplePostBoxRepository implements RiakSimplePostBoxRepository  {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultRiakSimplePostBoxRepository.class);

    private final static Timer COMMIT_TIMER = TimingReports.newTimer("postBoxRepo-commit");
    private final static Timer GET_BY_ID_TIMER = TimingReports.newTimer("postBoxRepo-getById");
    private final static Timer DELETE_TIMER = TimingReports.newTimer("postBoxRepo-delete");

    public static final String UPDATED_INDEX = "modifiedAt";
    public static final String POST_BOX = "postbox";

    @Autowired
    private Converter<PostBox> converter;

    @Autowired
    private ConflictResolver<PostBox> resolver;

    @Autowired
    private RiakSimplePostBoxMerger postBoxMerger;

    @Autowired
    private IRiakClient riakClient;

    @Value("${persistence.riak.bucket.name.prefix:}" + POST_BOX)
    private String bucketName;

    protected Bucket postBoxBucket;

    @PostConstruct
    public void createBucket() {
        try {
            this.postBoxBucket = riakClient.fetchBucket(bucketName).execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PostBox byId(PostBoxId id) {
        try (Timer.Context ignored = GET_BY_ID_TIMER.time()) {
            PostBox postBox = postBoxBucket
              .fetch(id.asString(), PostBox.class)
              .withConverter(converter)
              .withResolver(resolver)
              .notFoundOK(true)
              .r(Quora.QUORUM)
              .withRetrier(new DefaultRetrier(3))
              .execute();

            if (postBox == null) {
                LOG.debug("Found no Postbox in Riak, returning an empty one for id {}", id.asString());
                postBox = new PostBox(id.asString(), Optional.of(0L), Lists.newArrayList());
            }

            LOG.debug("Found {} threads ({} unread) for PostBox with id {} in Riak", postBox.getConversationThreads().size(),
                    postBox.getNewRepliesCounter().getValue(), id.asString());
            return postBox;
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not load post-box by id #" + id.asString(), e);
        }
    }

    @Override
    public PostBox byIdWithoutConversationThreads(PostBoxId id) {
        return byId(id);
    }

    @Override
    public void write(PostBox postBox) {
        write(postBox, Collections.emptyList());
    }

    @Override
    public void deleteConversations(PostBox postBox, List<String> deletedIds) {
        write(postBox, deletedIds);
    }

    @Override
    public void markConversationsAsRead(PostBox postBox, List<AbstractConversationThread> conversation) {
        write(postBox);
    }

    private void write(PostBox postBox, List<String> deletedIds) {
        try (Timer.Context ignored = COMMIT_TIMER.time()) {
            postBoxBucket.store(postBox.getEmail(), postBox)
              .withConverter(converter)
              .withResolver(resolver)
              .withMutator(new RiakSimplePostBoxMutator(postBoxMerger, postBox, deletedIds))
              .returnBody(false)
              .w(Quora.QUORUM)
              .execute();
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException("could not write post-box #" + postBox.getEmail(), e);
        }
    }

    @Override
    public boolean cleanup(DateTime time) {
        try {
            StreamingOperation<IndexEntry> keyStream = postBoxBucket.fetchIndex(IntIndex.named(UPDATED_INDEX))
              .from(0)
              .to(time.getMillis())
              .executeStreaming();

            LOG.info("Cleaning up Riak PostBoxes before {}", time);

            int counter = 0;

            for (IndexEntry indexEntry : keyStream) {
                try {
                    delete(indexEntry.getObjectKey());

                    if (counter % 1000 == 0) {
                        LOG.info("Iterated postbox to cleanup number: " + counter);
                    }
                } catch (RuntimeException e) {
                    LOG.error("Cleanup: could not cleanup postbox: " + indexEntry.getObjectKey(), e);
                } finally {
                    counter++;
                }
            }

            LOG.info("finished postbox cleanup overall deleted items: " + counter);
        } catch (RiakException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    @Override
    public Optional<AbstractConversationThread> threadById(PostBoxId id, String conversationId) {
        return byId(id).lookupConversation(conversationId);
    }

    @Override
    public Long upsertThread(PostBoxId id, AbstractConversationThread conversationThread, boolean markAsUnread) {
        PostBox<AbstractConversationThread> postBox = byId(id);

        List<AbstractConversationThread> finalThreads = postBox.getConversationThreads().stream()
          .filter(thread -> !thread.getConversationId().equals(conversationThread.getConversationId()))
          .collect(Collectors.toList());

        if (markAsUnread) {
            postBox.incNewReplies();
        }

        finalThreads.add(conversationThread);

        // Write the PostBox to the repository

        PostBox postBoxToWrite = new PostBox(id.asString(), Optional.of(postBox.getNewRepliesCounter().getValue()), finalThreads);

        write(postBoxToWrite);

        return (long) postBoxToWrite.getUnreadConversations().size();
    }

    private void delete(String email) {
        try(Timer.Context ignored = DELETE_TIMER.time()) {
            postBoxBucket.delete(email).execute();
        } catch (RiakException e) {
            LOG.error("could not delete post box", e);
        }
    }

    @Override
    public long getMessagesCount(DateTime fromDate, DateTime toDate) {
        return streamPostBoxIds(fromDate, toDate).count();
    }

    @Override
    public Stream<String> streamPostBoxIds(DateTime fromDate, DateTime toDate) { // use endDate as its current date
        LOG.debug("Fetching Postboxes modifiedBetween {} - {}", fromDate, toDate);

        try {

            Spliterator<IndexEntry> idxSplitterator =  postBoxBucket.fetchIndex(IntIndex.named(UPDATED_INDEX))
                    .from(fromDate.getMillis())
                    .to(toDate.getMillis())
                    .executeStreaming()
                    .spliterator();

            return StreamSupport.stream(idxSplitterator, false).map(idx -> idx.getObjectKey());

        } catch (RiakException e) {

            String errMess = "Streaming postboxes modified between '" + fromDate + "' and '" + toDate + "' failed";
            LOG.error(errMess, e);
            throw new RuntimeException(errMess, e);
        }
    }

    @Override
    public List<String> getPostBoxIds(DateTime fromDate, DateTime toDate) {
         return streamPostBoxIds(fromDate, toDate).collect(Collectors.toList());
    }

    /**
     * TODO: keep the same functionalitu in Riak during a migration.
     */
    @Override
    public int unreadCountInConversation(PostBoxId id, String conversationId) {
        return 1;
    }

    @Override
    public int unreadCountInConversations(PostBoxId id, List<AbstractConversationThread> conversations) {
        PostBox<AbstractConversationThread> postBox = byId(id);

        return conversations.stream()
                .filter(postBox::containsConversation)
                .mapToInt(conversation -> unreadCountInConversation(id, conversation.getConversationId()))
                .sum();
    }
}
