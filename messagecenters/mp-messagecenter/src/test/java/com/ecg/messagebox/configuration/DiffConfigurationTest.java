package com.ecg.messagebox.configuration;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DiffConfigurationTest {

    private static final String USER_ID = "1";

    @Test
    public void testForNull() {
        DiffConfiguration config = new DiffConfiguration(null, null);
        assertThat(config.useDiff(USER_ID), is(false));
    }

    @Test
    public void testForEmpty() {
        DiffConfiguration config = new DiffConfiguration("", "");
        assertThat(config.useDiff(USER_ID), is(false));
    }

    @Test
    public void testForZeroRange() {
        DiffConfiguration config = new DiffConfiguration("0-0", "");
        assertThat(config.useDiff(USER_ID), is(false));
    }

    @Test
    public void testForUserIdInRange() {
        DiffConfiguration config = new DiffConfiguration("0-60", "");
        assertThat(config.useDiff(USER_ID), is(true));
    }

    @Test
    public void testForUserIdNotInRange() {
        DiffConfiguration config = new DiffConfiguration("60-100", "");
        assertThat(config.useDiff(USER_ID), is(false));
    }

    @Test
    public void testForUserIdInList() {
        DiffConfiguration config = new DiffConfiguration("", "1,2");
        assertThat(config.useDiff(USER_ID), is(true));
    }

    @Test
    public void testForUserIdNotInList() {
        DiffConfiguration config = new DiffConfiguration("", "2,3");
        assertThat(config.useDiff(USER_ID), is(false));
    }

    @Test
    public void testForUserIdInListOnly() {
        DiffConfiguration config = new DiffConfiguration("60-100", "1,2");
        assertThat(config.useDiff(USER_ID), is(true));
    }

    @Test
    public void testForUserIdInBoth() {
        DiffConfiguration config = new DiffConfiguration("0-60", "1,2");
        assertThat(config.useDiff(USER_ID), is(true));
    }

    @Test
    public void testForUserIdInNone() {
        DiffConfiguration config = new DiffConfiguration("60-100", "2,3");
        assertThat(config.useDiff(USER_ID), is(false));
    }
}