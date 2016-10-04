package com.ecg.messagecenter.persistence.simple;

import com.basho.riak.client.bucket.Bucket;
import com.ecg.messagecenter.persistence.AbstractConversationThread;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.reflect.Proxy;
import java.util.List;

public class RiakReadOnlySimplePostBoxRepository extends DefaultRiakSimplePostBoxRepository {
    private static final Logger LOG = LoggerFactory.getLogger(RiakReadOnlySimplePostBoxRepository.class);

    @PostConstruct
    public void createBucketProxy() {
        // Wrap the Riak bucket in an intercepting Proxy to ensure we never accidentally write to Riak

        postBoxBucket = (Bucket) Proxy.newProxyInstance(postBoxBucket.getClass().getClassLoader(),
          new Class[] { Bucket.class },
          (proxy, method, args) -> {
            if (method.getName().contains("store") || method.getName().contains("delete")) {
                throw new IllegalStateException("Read-only Riak SimplePostBoxRepository attempted to perform write operation");
            }

            return method.invoke(args);
          });
    }

    @Override
    public void write(PostBox postBox) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.write was called");
    }

    @Override
    public void write(PostBox postBox, List<String> deletedIds) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.write was called");
    }

    @Override
    public void cleanup(DateTime time) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.cleanup was called");
    }

    @Override
    public Long upsertThread(String email, AbstractConversationThread conversationThread, boolean markAsUnread) {
        LOG.debug("RiakReadOnlySimplePostBoxRepository.upsertThread was called");

        // Don't update but do retrieve the existing PostBox in order to return the correct number of unread threads

        PostBox<AbstractConversationThread> existingPostBox = byId(email);

        return existingPostBox.getNewRepliesCounter().getValue();
    }
}