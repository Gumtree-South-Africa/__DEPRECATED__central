package com.ecg.replyts;

import com.googlecode.junittoolbox.ParallelSuite;
import com.googlecode.junittoolbox.SuiteClasses;
import org.junit.runner.RunWith;

/**
 * This class configures JUnit to run some tests in parallel and it
 * can be used as an utility to run tests in parallel from IntelliJ. It is not used by maven
 */
@RunWith(ParallelSuite.class)
@SuiteClasses({ "**/*Test.class" })
public class ParallelTestRunner {
}
