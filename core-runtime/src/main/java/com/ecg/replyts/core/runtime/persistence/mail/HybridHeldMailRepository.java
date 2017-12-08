package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.persistence.HeldMailRepository;
import com.ecg.replyts.core.api.persistence.MessageNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HybridHeldMailRepository implements HeldMailRepository {

    private static final Logger LOG = LoggerFactory.getLogger(HybridHeldMailRepository.class);

    private final CassandraHeldMailRepository cassandraHeldMailRepository;
    private final RiakHeldMailRepository riakHeldMailRepository;

    public HybridHeldMailRepository(CassandraHeldMailRepository cassandraHeldMailRepository, RiakHeldMailRepository riakHeldMailRepository) {
        this.cassandraHeldMailRepository = cassandraHeldMailRepository;
        this.riakHeldMailRepository = riakHeldMailRepository;
    }

    @Override
    public byte[] read(String messageId) throws MessageNotFoundException {
        try {
            return cassandraHeldMailRepository.read(messageId);
        } catch (MessageNotFoundException e1) {
            try {
                byte[] content = riakHeldMailRepository.read(messageId);
                cassandraHeldMailRepository.write(messageId, content);
                return content;
            } catch (MessageNotFoundException e2) {
                LOG.warn("Message with id {} was not find both in Riak and Cassandra", messageId, e1, e2);
                return null;
            }
        }
    }

    @Override
    public void write(String messageId, byte[] content) {
        cassandraHeldMailRepository.write(messageId, content);
        // No need to call write() for Riak; it does nothing because reads are delegated to the MailRepository
    }

    @Override
    public void remove(String messageId) {
        cassandraHeldMailRepository.remove(messageId);
        // No need to call write() for Riak; it does nothing because reads are delegated to the MailRepository
    }
}
