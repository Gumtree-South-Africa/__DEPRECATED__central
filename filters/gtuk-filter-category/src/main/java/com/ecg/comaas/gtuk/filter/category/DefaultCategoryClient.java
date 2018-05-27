package com.ecg.comaas.gtuk.filter.category;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.function.BiPredicate;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DefaultCategoryClient implements CategoryClient {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCategoryClient.class);

    private static final BiPredicate<Response, Exception> FAILED_INVOCATION = (response, ex) -> {
       if (ex != null) {
           LOG.warn("CategoryClient invocation failed with an exception.", ex.getMessage());
           return false;
       }

       if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
           LOG.warn("CategoryClient invocation failed with a wrong status code: " + response.getStatus());
           return false;
       }

       return true;
    };

    private static final CategoryComparator CATEGORY_COMPARATOR = new CategoryComparator();

    private final JerseyClient client;
    private final RetryPolicy retryPolicy;
    private final WebTarget versionTarget;
    private final WebTarget categoryTarget;

    public DefaultCategoryClient(String baseUri, int port, int socketTimeout, int connectionTimeout, int retries) {
        this.retryPolicy = new RetryPolicy()
                .withDelay(500, TimeUnit.MILLISECONDS)
                .withMaxRetries(retries)
                .retryIf(FAILED_INVOCATION);

        this.client = JerseyClientBuilder.createClient();
        this.client.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout);
        this.client.property(ClientProperties.READ_TIMEOUT, socketTimeout);
        this.client.register(new JacksonFeature());

        UriBuilder finalUri = UriBuilder.fromPath(baseUri).port(port).scheme("http");
        this.versionTarget = client.target(finalUri).path("_version");
        this.categoryTarget = client.target(finalUri).path("api/categories");
    }

    public Optional<Category> categoryTree() {
        Response response = Failsafe.with(retryPolicy).get(() -> categoryTarget.request().get());

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            Category category = response.readEntity(Category.class);
            return Optional.of(sortCategoryTree(category));
        } else {
            LOG.warn("Could not get current CATEGORY TREE");
            return Optional.empty();
        }
    }

    public Optional<String> version() {
        Response response = Failsafe.with(retryPolicy).get(() -> versionTarget.request().get());

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            List<String> versions = response.readEntity(new GenericType<List<String>>() {});
            return Optional.of(versions.get(0));
        } else {
            LOG.warn("Could not get current CATEGORY VERSION");
            return Optional.empty();
        }
    }

    private static Category sortCategoryTree(Category category) {
        if (!category.getChildren().isEmpty()) {
            sortCategoryChildren(category);
            for (Category c : category.getChildren()) {
                sortCategoryTree(c);
            }
        }

        return category;
    }

    private static Category sortCategoryChildren(Category category) {
        List<Category> unsorted = category.getChildren();
        ImmutableList<Category> categories = Ordering.from(CATEGORY_COMPARATOR).immutableSortedCopy(unsorted);
        category.setChildren(categories);
        return category;
    }

    @Override
    public void close() throws Exception {
        client.close();
    }
}