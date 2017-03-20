package com.ecg.comaas.r2cmigration.difftool;

import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractDiffTool {
    private Logger LOG = LoggerFactory.getLogger(AbstractDiffTool.class);
    DateTime startDate;
    DateTime endDate;
    int maxEntityAge;
    int tzShiftInMin;

    final void setDateRange(DateTime startDate, DateTime endDate, int tzShiftInMin) {
        this.tzShiftInMin = tzShiftInMin;

        this.endDate = endDate == null ? new DateTime(DateTimeZone.UTC) : endDate;
        this.startDate = startDate == null ? this.endDate.minusDays(maxEntityAge) : startDate;
        if (!this.endDate.isBeforeNow()) {
            LOG.warn("Difftool will fail because endDate is not before now(), which is {}", DateTime.now(DateTimeZone.UTC));
            throw new IllegalStateException("Incorrect parameters");
        }
        Preconditions.checkArgument(this.startDate.isBefore(this.endDate));
        LOG.info("Compare between {} and {}", this.startDate, this.endDate);
    }
}
