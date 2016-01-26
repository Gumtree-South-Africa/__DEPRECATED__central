package com.ecg.replyts.core.runtime.sanitycheck.adapter;

import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.Message;
import com.ecg.replyts.core.api.sanitychecks.Result;
import com.ecg.replyts.core.api.sanitychecks.Status;
import org.junit.Test;

import java.util.concurrent.Semaphore;

import static org.junit.Assert.assertEquals;


public class CheckAdapterTest {

    @Test
    public void testBlockingRequest() {

        final Semaphore continueSignal = new Semaphore(0);

        SingleCheckAdapter check = new SingleCheckAdapter(new Check() {

            public Result execute() throws Exception {
                // simulate the hanging
                continueSignal.acquire();
                // return OK too late after timeout!
                return Result.createResult(Status.OK, Message.shortInfo(""));
            }

            public String getCategory() {
                return "c";
            }

            public String getName() {
                return "n";
            }

            public String getSubCategory() {
                return "sc";
            }
        });
        check.setTimeout(200);

        check.execute();
        Result result = check.getLatestResult();
        assertEquals(Status.CRITICAL, result.status());

        check.execute();
        result = check.getLatestResult();
        assertEquals(Status.CRITICAL, result.status());

        // release the hanging check!
        continueSignal.release();
    }

    @Test
    public void testSuccessfullRequest() {

        SingleCheckAdapter check = new SingleCheckAdapter(new Check() {

            public Result execute() throws Exception {
                return Result.createResult(Status.OK, Message.shortInfo(""));
            }

            public String getCategory() {
                return "c";
            }

            public String getName() {
                return "n";
            }

            public String getSubCategory() {
                return "sc";
            }
        });
        // Try to execute the check several time, no blocking expected
        for (int i = 0; i < 10; i++) {
            check.execute();
            Result result = check.getLatestResult();
            assertEquals(Status.OK, result.status());
        }
    }

}
