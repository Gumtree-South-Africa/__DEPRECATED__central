package com.ecg.replyts.core.runtime.persistence.mail;

import com.basho.riak.client.IRiakClient;
import com.basho.riak.client.RiakRetryFailedException;
import com.ecg.replyts.core.runtime.persistence.HybridMigrationClusterState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConditionalOnExpression("#{'${persistence.strategy}'.startsWith('hybrid') && '${swift.attachment.storage.enabled:false}'.equalsIgnoreCase('true') }")
public class HybridMailConfiguration {

    @Autowired
    public HybridMigrationClusterState migrationState;

    @Autowired
    public IRiakClient riakClient;

    @Bean
    DiffingRiakMailRepository diffingRiakMailRepository(@Value("${persistence.riak.bucket.name.prefix:}") String bucketPrefix, IRiakClient riakClient) {
        try {
            return new DiffingRiakMailRepository(bucketPrefix, riakClient);
        } catch (RiakRetryFailedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Bean
    public HybridMailRepository hybridMailRepository() {
        return new HybridMailRepository();
    }
}
