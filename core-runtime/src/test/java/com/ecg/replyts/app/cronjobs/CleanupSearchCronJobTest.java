package com.ecg.replyts.app.cronjobs;

import com.ecg.replyts.core.api.search.MutableSearchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.ParseException;
import java.time.LocalDate;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CleanupSearchCronJobTest {

    @Mock
    private CleanupConfiguration config;

    @Mock
    private MutableSearchService searchService;

    private CleanupSearchCronJob cleanupSearchCronJob;
    private LocalDate now;
    private LocalDate oneDayBeforeNow;

    @Before
    public void setUp() throws ParseException {
        when(config.getMaxConversationAgeDays()).thenReturn(1);
        now = LocalDate.of(2017, 12, 2);
        oneDayBeforeNow = LocalDate.of(2017, 12, 1);

        cleanupSearchCronJob = new CleanupSearchCronJob(config, searchService) {
            @Override
            protected LocalDate now() {
                return now;
            }
        };
    }

    @Test
    public void execute() throws Exception {
        cleanupSearchCronJob.execute();

        verify(searchService).deleteModifiedAt(eq(LocalDate.of(1970, 1, 1)), eq(oneDayBeforeNow));
    }
}