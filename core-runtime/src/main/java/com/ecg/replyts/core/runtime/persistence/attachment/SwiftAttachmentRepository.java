package com.ecg.replyts.core.runtime.persistence.attachment;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;


import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.*;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.netty.handler.codec.http.HttpResponseStatus;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.exceptions.AuthenticationException;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.core.transport.HttpResponse;
import org.openstack4j.model.identity.v2.Access;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectListOptions;
import org.openstack4j.openstack.OSFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

// Interface to Swift repository for reading attachments
public class SwiftAttachmentRepository {


    private static final Logger LOG = LoggerFactory.getLogger(SwiftAttachmentRepository.class);

    private static final Timer GET = TimingReports.newTimer("attachment-get-timer");
    private static final Timer GET_NAMES = TimingReports.newTimer("attachment-getnames-timer");

    private OSClient.OSClientV2 swiftClient;

    @Value("#{systemEnvironment['swift_authentication_url']}")
    private String keystone;

    @Value("${replyts.maxConversationAgeDays:180}")
    private int retentionDays;

    @Value("${swift.username:comaas-qa-swift}")
    private String username;

    @Value("${swift.password:changeme}")
    private String passwd;

    @Value("${swift.tenant:comaas-qa}")
    private String tenantName;

    // Should be a prime number
    // https://primes.utm.edu/lists/small/1000.txt
    @Value("${swift.bucket.number:97}")
    private int numberOfBuckets;

    @Value("${replyts.swift.container:test-container}")
    private String containerPrefix;

    private final HashFunction hf = Hashing.murmur3_128();

    @PostConstruct
    private void connect() {
        // V2 authentication
        LOG.debug("Connecting to Swift storage endpoint {}, with username '{}', passwd '{}' and tenant '{}' ", keystone, username, passwd, tenantName);
        try {

            if(StringUtils.isEmpty(keystone)) {
                LOG.error("Missing 'swift_authentication_url' environment variable (should be the keystone URL). Swift is disabled");
                return;
            }
            swiftClient = OSFactory.builderV2()
                    .endpoint(keystone)
                    .credentials(username, passwd)
                    .tenantName(tenantName)
                    .authenticate();

        } catch (AuthenticationException e) {
            swiftClient = null;
            LOG.error("Failed to connect to Swift", e);
        }

    }

    public String getContainer(String messageid) {
        Preconditions.checkNotNull(messageid);
        String containerName = containerPrefix + "-" + getBucket(messageid);
        LOG.debug("Resolved container name: {} ", containerName);
        return containerName;
    }

    int getBucket(String messageid) {
        HashCode hc = hf.newHasher().putString(messageid, Charsets.UTF_8).hash();
        // would be nice to use Hashing.consistentHash(hc, numberOfBuckets); for consistency
        // but that does not solve our problem completely, yet makes it harder to achieve
        // portability across the languages
        return Math.abs(hc.asInt() % numberOfBuckets);
    }

    public Optional<SwiftObject> fetch(String messageId, String attachmentName) {
        try (Timer.Context ignored = GET.time()) {
            LOG.debug("Fetching messageid '{}' attachment '{}'", messageId, attachmentName);
            String containerName = getContainer(messageId);
            SwiftObject so = getObjectStorage().get(containerName, messageId + "/" + attachmentName);
            if (so == null) {
                return Optional.empty();
            }
            HttpResponse resp = so.download().getHttpResponse();
            if (resp.getStatus() != HttpResponseStatus.OK.getCode()) {

                String mess = String.format("Failed to fetch %s/%s attachment ", messageId, attachmentName);
                throw new RuntimeException(mess + " Reason: " + resp.getStatusMessage());
            }
            // This one does not contain MD5, unlike the one from getNames!?
            return Optional.of(so);
        }
    }

    // ObjectStorageObjectService is not thread safe, we need to have one per thread
    private ObjectStorageObjectService getObjectStorage() {
        checkState(swiftClient != null, "Not connected to Swift");
        Access access = swiftClient.getAccess();
        OSClient.OSClientV2 clientV2 = OSFactory.clientFromAccess(access);
        return clientV2.objectStorage().objects();
    }

    public Optional<Map<String, SwiftObject>> getNames(String messageId) {
        try (Timer.Context ignored = GET_NAMES.time()) {

            LOG.debug("Listing attachments under messageid '{}' ", messageId);
            ObjectListOptions opts = ObjectListOptions.create().delimiter('/').path(messageId);

            String containerName = getContainer(messageId);
            List<? extends SwiftObject> paths = getObjectStorage().list(containerName, opts);
            Map<String, SwiftObject> omap = paths.stream().collect(Collectors.toMap(SwiftObject::getName, Function.identity()));

            if (omap.keySet().size() > 0) {
                LOG.debug("{} attachments found for messageid '{}' ", omap.keySet().size(), messageId);
            }
            return Optional.ofNullable(omap);
        }
    }

    int getNumberOfBuckets() {
        return numberOfBuckets;
    }

}
