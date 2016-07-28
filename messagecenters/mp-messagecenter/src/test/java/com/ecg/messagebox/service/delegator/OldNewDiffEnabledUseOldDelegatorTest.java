package com.ecg.messagebox.service.delegator;

import com.ecg.messagebox.service.PostBoxServiceDelegator;
import org.junit.Before;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * PostBoxDelegator test class which verifies behaviour when:
 * <ul>
 * <li>read/write operations for both old and new data model are enabled;
 * <li>diff tool is enabled;
 * <l1>data from the old data model is being returned to the caller.
 * </ul>
 */
public class OldNewDiffEnabledUseOldDelegatorTest extends BaseDelegatorTest {

    private PostBoxServiceDelegator delegator = new PostBoxServiceDelegator(oldPbService, newPbService,
            pbRespConverter, convRespConverter, unreadCountsConverter, diff, newModelConfig, diffConfig, true, MESSAGES_LIMIT);

    @Before
    public void setup() {
        when(newModelConfig.newModelEnabled(anyString())).thenReturn(true);
        when(newModelConfig.useNewModel(anyString())).thenReturn(false);
        when(diffConfig.useDiff(anyString())).thenReturn(true);
    }
}
