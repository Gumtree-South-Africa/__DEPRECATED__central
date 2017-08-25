package com.ecg.replyts.core.runtime.persistence.attachment;

import com.datastax.driver.core.utils.UUIDs;
import com.ecg.replyts.core.runtime.cluster.Guids;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = SwiftAttachmentRepositoryTest.TestContext.class)
@TestPropertySource(properties = {
        "swift.bucket.number = 13",
        "swift.username:comaas-qa-swift",
        "swift.password:Z6J$6QfV@HU1%aGv",
        "swift.tenant:comaas-qa"
})
public class SwiftAttachmentRepositoryTest {

    @Autowired
    private SwiftAttachmentRepository swiftAttachmentRepository;

    @Autowired
    private Guids guids;

    @Test(timeout = 20000L)
    public void testHashMessageIntoIds() {
        try (InputStream is = getClass().getResourceAsStream("/riak_ids.txt")) {
            // read some riak Ids
            Map<String, AtomicInteger> bucketCount = new HashMap<>();
            // bucket -> values
            Multimap<String, String> values = ArrayListMultimap.create();
            Scanner s = new Scanner(is);
            int counter = 0;
            while (s.hasNext()) {
                String messageId = s.next();
                counter++;

                String container = swiftAttachmentRepository.getContainer(messageId);
                bucketCount.putIfAbsent(container, new AtomicInteger(0));
                values.put(container, messageId);
                bucketCount.get(container).incrementAndGet();
            }
            // Generate some message ids
            for (int i = 0; i < 100; i++) {
                String messageId = guids.nextGuid();
                counter++;
                String container = swiftAttachmentRepository.getContainer(messageId);
                values.put(container, messageId);
                bucketCount.putIfAbsent(container, new AtomicInteger(0));
                bucketCount.get(container).incrementAndGet();
            }
            // Generate some timebased ids
            for (int i = 0; i < 100; i++) {
                String messageId = UUIDs.timeBased().toString();
                counter++;
                String container = swiftAttachmentRepository.getContainer(messageId);
                values.put(container, messageId);
                bucketCount.putIfAbsent(container, new AtomicInteger(0));
                bucketCount.get(container).incrementAndGet();
            }

            assertEquals("Number of keys does not match number of buckets", swiftAttachmentRepository.getNumberOfBuckets(), bucketCount.keySet().size());

            // make sure they are evently dispersed in swiftAttachmentRepository.
            int avgitemsPerBucket = (int) counter / swiftAttachmentRepository.getNumberOfBuckets();

            for (String key : bucketCount.keySet()) {
                String msg = String.format("Number of items per bucket %d significantly differs from average %d", avgitemsPerBucket, bucketCount.get(key).get());
                assertTrue(msg, (avgitemsPerBucket / (float) bucketCount.get(key).get()) > 0.6);
            }

            // Test hash consistency (takes 1s)
            for (String v : values.keys()) {
                Collection<String> ids = values.get(v);
                for (String id : ids) {
                    Assert.assertEquals(v, swiftAttachmentRepository.getContainer(id));
                }
            }

        } catch (IOException ioe) {
            fail(ioe.getMessage());
        }
    }


    @Configuration
    @Import(AttachmentConfig.class)
    static class TestContext {
        @Bean
        SwiftAttachmentRepository swiftAttachmentRepository() {
            return new SwiftAttachmentRepository();
        }

        @Bean
        Guids guids() {
            return new Guids();
        }
    }
}
