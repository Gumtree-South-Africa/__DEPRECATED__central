package com.ecg.comaas.gtuk.filter.category;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CategoryChangeMonitor implements ApplicationListener<ContextRefreshedEvent>, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CategoryChangeMonitor.class);

    private String currentVersion;
    private final CategoryClient categoryClient;
    private final int checkInterval;
    private final Reloadable<Category> reloadable;
    private final ScheduledExecutorService scheduler;

    public CategoryChangeMonitor(String version, CategoryClient categoryClient, int checkInterval, Reloadable<Category> reloadable) {
        this.currentVersion = version;
        this.categoryClient = categoryClient;
        this.checkInterval = checkInterval;
        this.reloadable = reloadable;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Load changes at the start of an application
        checkForChanges();
        // Start a recurrent update of an internal state
        startMonitor();
    }

    private void startMonitor() {
        scheduler.schedule(this::checkForChanges, checkInterval, TimeUnit.MILLISECONDS);
    }

    private void checkForChanges() {
        Optional<String> optVersion = categoryClient.version();

        if (optVersion.isPresent()) {
            String obtainedVersion = optVersion.get();
            if (currentVersion == null || currentVersion.equals(obtainedVersion)) {
                handleNewVersion(obtainedVersion);
            }
        }
    }

    private void handleNewVersion(String newVersion) {
        LOG.info("Category changes detected. Going to notify registered change listeners.");
        Optional<Category> optCategory = categoryClient.categoryTree();

        if (optCategory.isPresent()) {
            this.currentVersion = newVersion;
            this.reloadable.reload(optCategory.get());
        }
    }

    @Override
    public void close() throws Exception {
        scheduler.shutdown();
    }
}