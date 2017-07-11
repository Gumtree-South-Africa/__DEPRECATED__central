package com.ecg.replyts.core.runtime.sanitycheck;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.CheckProvider;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

public class SanityCheckServiceTest {
    private SanityCheckService s;

    private Check mockCheck = new Check() {
        public String getSubCategory() {
            // TODO Auto-generated method stub
            return "m";
        }

        public String getName() {
            // TODO Auto-generated method stub
            return "Mock";
        }

        public String getCategory() {
            // TODO Auto-generated method stub
            return "o";
        }

        public Result execute() throws Exception {
            return Result.createResult(Status.OK, Message.EMPTY);
        }
    };

    @Test
    public void launchesSanityCheckFramework() throws Exception {
        CheckProvider cp = Mockito.mock(CheckProvider.class);

        Mockito.when(cp.getChecks()).thenReturn(Arrays.asList(mockCheck));

        s = new SanityCheckService();

        ReflectionTestUtils.setField(s, "providers", Arrays.asList(cp));

        s.addChecksAndStart();
        s.stop();
    }
}