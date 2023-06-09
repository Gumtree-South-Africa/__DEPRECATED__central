package com.ecg.replyts.core.runtime.persistence.attachment;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.escape.Escaper;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.net.UrlEscapers;
import org.apache.commons.lang3.StringUtils;
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
import javax.ws.rs.core.Response;
import java.io.IOException;
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

    @Value("${swift.authentication.url:}")
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
    private static final Escaper urlPathEscaper = UrlEscapers.urlPathSegmentEscaper();

    @PostConstruct
    private void connect() {
        // V2 authentication
        LOG.debug("Connecting to Swift storage endpoint {}, with username '{}', passwd '{}' and tenant '{}' ", keystone, username, passwd, tenantName);
        if (StringUtils.isEmpty(keystone)) {
            LOG.error("Missing 'swift.authentication.url' property. Swift is disabled");
            return;
        }
        try {
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
        LOG.trace("Resolved container name: {} ", containerName);
        return containerName;
    }

    private int getBucket(String messageid) {
        HashCode hc = hf.newHasher().putString(messageid, Charsets.UTF_8).hash();
        // would be nice to use Hashing.consistentHash(hc, numberOfBuckets); for consistency
        // but that does not solve our problem completely, yet makes it harder to achieve
        // portability across the languages
        return Math.abs(hc.asInt() % numberOfBuckets);
    }

    public Optional<SwiftObject> fetch(String messageId, String attachmentName) {

        String containerName = getContainer(messageId);

        try (Timer.Context ignored = GET.time()) {

            LOG.debug("Fetching messageid '{}' attachment '{}' from container {}", messageId, attachmentName, containerName);

            SwiftObject so = getObjectStorage().get(containerName,
                    urlPathEscaper.escape(messageId) + "/" + urlPathEscaper.escape(attachmentName));

            if (so == null) {
                LOG.info("Did not find messageid '{}' attachment '{}' in container {}", messageId, attachmentName, containerName);
                return Optional.empty();
            }
            try (HttpResponse resp = so.download().getHttpResponse()) {
                if (resp.getStatus() != Response.Status.OK.getStatusCode()) {
                    String mess = String.format("Failed to fetch %s/%s attachment from container %s", messageId, attachmentName, containerName);
                    throw new RuntimeException(mess + " Reason: " + resp.getStatusMessage());
                }
                Optional<SwiftObject> swiftObject = Optional.of(so);

                if (LOG.isDebugEnabled()) {
                    SwiftObject sobj = swiftObject.get();
                    LOG.debug("Loaded attachment {}/{} size {} bytes, " +
                                    "from container {}, " +
                                    "lastModifiedDate {}, " +
                                    "mimeType {}", messageId, attachmentName, sobj.getSizeInBytes(),
                            sobj.getContainerName(),
                            sobj.getLastModified(),
                            sobj.getMimeType());
                }

                return swiftObject;
            } catch (IOException e) {
                LOG.error("IOException while closing the response on SwiftObject for messageid '{}' attachment '{}' in container {}", messageId, attachmentName, containerName, e);
            }
            return Optional.empty();
        }
    }

    /* ObjectStorageObjectService is not thread safe, we need to have one per thread
      ONLY USE this method directly FOR TESTING! */
    @VisibleForTesting
    public ObjectStorageObjectService getObjectStorage() {
        checkState(swiftClient != null, "Not connected to Swift");
        Access access = swiftClient.getAccess();
        OSClient.OSClientV2 clientV2 = OSFactory.clientFromAccess(access);
        return clientV2.objectStorage().objects();
    }

    public Optional<Map<String, SwiftObject>> getNames(String messageId) {
        try (Timer.Context ignored = GET_NAMES.time()) {

            LOG.debug("Listing attachments under messageid '{}' ", messageId);
            ObjectListOptions opts = ObjectListOptions.create().delimiter('/').path(urlPathEscaper.escape(messageId));

            String containerName = getContainer(messageId);
            List<? extends SwiftObject> paths = getObjectStorage().list(containerName, opts);
            Map<String, SwiftObject> omap = paths.stream().collect(Collectors.toMap(SwiftObject::getName, Function.identity()));

            if (omap.keySet().size() > 0) {
                LOG.debug("{} attachments found for messageid '{}' ", omap.keySet().size(), messageId);
            } else {
                LOG.debug("Did not find any attachments for messageid '{}' in container {} ", messageId, containerName);
            }
            return Optional.of(omap);
        }
    }

    int getNumberOfBuckets() {
        return numberOfBuckets;
    }

}
