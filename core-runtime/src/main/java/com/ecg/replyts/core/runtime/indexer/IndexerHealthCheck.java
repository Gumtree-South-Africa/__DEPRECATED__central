package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.CheckProvider;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import com.google.common.collect.ImmutableList;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class IndexerHealthCheck implements CheckProvider {
    private final AtomicReference<Result> fullImportResult = new AtomicReference<Result>();

    private final AtomicReference<Result> deltaImportResult = new AtomicReference<Result>();

    private final List<Check> checks = ImmutableList.<Check>of(
      new AsyncIndexerHealthCheck(fullImportResult, "fullImport"),
      new AsyncIndexerHealthCheck(deltaImportResult, "deltaImport"));

    @Override
    public List<Check> getChecks() {
        return checks;
    }

    @PostConstruct
    public void initialize() {
        fullImportResult.set(Result.createResult(Status.OK, Message.shortInfo("No full import done yet")));
        deltaImportResult.set(Result.createResult(Status.OK, Message.shortInfo("No delta import done yet")));
    }

    public void reportFull(Status status, Message message) {
        fullImportResult.set(Result.createResult(status, message));
    }

    public void reportDelta(Status status, Message message) {
        deltaImportResult.set(Result.createResult(status, message));
    }

    private final class AsyncIndexerHealthCheck implements Check {
        private final AtomicReference<Result> lastResult;

        private final String indexerType;

        private AsyncIndexerHealthCheck(AtomicReference<Result> lastResult, String indexerType) {
            this.lastResult = lastResult;
            this.indexerType = indexerType;
        }

        @Override
        public Result execute() throws Exception {
            return lastResult.get();
        }

        @Override
        public String getName() {
            return indexerType;
        }

        @Override
        public String getCategory() {
            return "ElasticSearch";
        }

        @Override
        public String getSubCategory() {
            return "indexer";
        }
    }
}