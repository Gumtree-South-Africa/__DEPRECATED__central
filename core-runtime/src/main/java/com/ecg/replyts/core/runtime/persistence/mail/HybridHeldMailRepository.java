package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.persistence.HeldMailRepository;

public class HybridHeldMailRepository implements HeldMailRepository {
    private CassandraHeldMailRepository cassandraHeldMailRepository;

    private RiakHeldMailRepository riakHeldMailRepository;

    public HybridHeldMailRepository(CassandraHeldMailRepository cassandraHeldMailRepository, RiakHeldMailRepository riakHeldMailRepository) {
        this.cassandraHeldMailRepository = cassandraHeldMailRepository;
        this.riakHeldMailRepository = riakHeldMailRepository;
    }

    @Override
    public byte[] read(String messageId) {
        try {
            return cassandraHeldMailRepository.read(messageId);
        } catch (RuntimeException e) {
            byte[] content = riakHeldMailRepository.read(messageId);

            cassandraHeldMailRepository.write(messageId, content);

            return content;
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
