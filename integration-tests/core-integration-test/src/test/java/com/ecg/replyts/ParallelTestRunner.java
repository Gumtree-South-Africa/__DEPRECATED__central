package com.ecg.replyts;

import com.googlecode.junittoolbox.ParallelSuite;
import com.googlecode.junittoolbox.SuiteClasses;
import org.junit.runner.RunWith;

/**
 * This class configures JUnit to run some tests in parallel and it
 * can be used as an utility to run tests in parallel from IntelliJ. It is not used by maven
 */
@RunWith(ParallelSuite.class)
@SuiteClasses({
        // Exclude tests that cannot be run in parallel
        "!**/SetCustomValueTest.class",
        "!**/MessageFilteringAcceptanceTest.class",
        "!**/SearchServiceTest.class",
        "!**/MultiDomainAcceptanceTest.class",
        "!**/SunnyDayAcceptanceTest.class",
        "!**/MessageModerationAcceptanceTest.class",
        "!**/AnonymizesOutgoingMailAcceptanceTest.class",
        "!**/ConversationResumingTest.class",
        "!**/RemoveLeakyMailQuotationsFromOutgoingMailAcceptanceTest.class",
        "!**/PreprocessorRemoveIgnoreableMailsAcceptanceTest.class",
        "!**/AttachmentSupportTest.class",
        "!**/PreprocessorRemoveIgnorableMailsAcceptanceTest.class",
        "!**/ConfigApiAcceptanceTest.class",
        "!**/CloseConversationTest.class",
        // Include the rest
        "**/*Test.class"})
public class ParallelTestRunner {
}
