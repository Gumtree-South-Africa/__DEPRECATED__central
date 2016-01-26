package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Perform checks over all registered checks and aggregate the results.
 *
 * @author smoczarski
 */
public class CheckAdapterAggregator extends AbstractCheckAdapter implements CheckAdapterAggregatorMBean {
    public static final Result NO_CHECKS_DEFINED =
            Result.createResult(Status.WARNING, Message.shortInfo("No checks defined"));

    private final Collection<CheckAdapter> checkAdapters = new CopyOnWriteArrayList<CheckAdapter>();

    public void addCheckAdapter(CheckAdapter check) {
        checkAdapters.add(check);
    }

    public void addCheckAdapter(List<CheckAdapter> checks) {
        checkAdapters.addAll(checks);
    }

    public void removeCheckAdapter(CheckAdapter checkAdapter) {
        checkAdapters.remove(checkAdapter);
    }

    @Override
    protected Result performInternal() {
        if (checkAdapters.isEmpty()) return NO_CHECKS_DEFINED;
        Map<String, Result> results = new HashMap<String, Result>();

        for (CheckAdapter checkAdapter : checkAdapters) {
            checkAdapter.execute();
            results.put(checkAdapter.getName(), checkAdapter.getLatestResult());
        }
        return AggregatedCheckResult.aggregateResults(results);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "AllChecks";
    }

    /**
     * Release resources.
     */
    public void destroy() {
        List<CheckAdapter> copy = new ArrayList<CheckAdapter>(this.checkAdapters);
        this.checkAdapters.clear();
        for (CheckAdapter checkAdapter : copy) {
            checkAdapter.destroy();
        }
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public String getSubCategory() {
        return null;
    }

}
