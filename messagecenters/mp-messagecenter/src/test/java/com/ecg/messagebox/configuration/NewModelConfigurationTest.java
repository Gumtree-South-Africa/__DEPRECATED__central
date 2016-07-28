package com.ecg.messagebox.configuration;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class NewModelConfigurationTest {

    private static final String USER_ID = "1";

    @Test
    public void testForNull() {
        NewModelConfiguration config = new NewModelConfiguration(null, null, null, null);
        assertThat(config.newModelEnabled(USER_ID), is(false));
        assertThat(config.useNewModel(USER_ID), is(false));
    }

    @Test
    public void testForEmpty() {
        NewModelConfiguration config = new NewModelConfiguration("", "", "", "");
        assertThat(config.newModelEnabled(USER_ID), is(false));
        assertThat(config.useNewModel(USER_ID), is(false));
    }

    @Test
    public void testForZeroRange() {
        NewModelConfiguration config = new NewModelConfiguration("0-0", "", "0-0", "");
        assertThat(config.newModelEnabled(USER_ID), is(false));
        assertThat(config.useNewModel(USER_ID), is(false));
    }

    @Test
    public void testForUserIdInRange() {
        NewModelConfiguration config = new NewModelConfiguration("0-60", "", "0-60", "");
        assertThat(config.newModelEnabled(USER_ID), is(true));
        assertThat(config.useNewModel(USER_ID), is(true));
    }

    @Test
    public void testForUserIdNotInRange() {
        NewModelConfiguration config = new NewModelConfiguration("60-100", "", "60-100", "");
        assertThat(config.newModelEnabled(USER_ID), is(false));
        assertThat(config.useNewModel(USER_ID), is(false));
    }

    @Test
    public void testForUserIdInList() {
        NewModelConfiguration config = new NewModelConfiguration("", "1,2", "", "1,2");
        assertThat(config.newModelEnabled(USER_ID), is(true));
        assertThat(config.useNewModel(USER_ID), is(true));
    }

    @Test
    public void testForUserIdNotInList() {
        NewModelConfiguration config = new NewModelConfiguration("", "2,3", "", "2,3");
        assertThat(config.newModelEnabled(USER_ID), is(false));
        assertThat(config.useNewModel(USER_ID), is(false));
    }

    @Test
    public void testForUserIdInRangeOnly() {
        NewModelConfiguration config = new NewModelConfiguration("0-60", "2,3", "0-60", "2,3");
        assertThat(config.newModelEnabled(USER_ID), is(true));
        assertThat(config.useNewModel(USER_ID), is(true));
    }

    @Test
    public void testForUserIdInListOnly() {
        NewModelConfiguration config = new NewModelConfiguration("60-100", "1,2", "60-100", "1,2");
        assertThat(config.newModelEnabled(USER_ID), is(true));
        assertThat(config.useNewModel(USER_ID), is(true));
    }

    @Test
    public void testForUserIdInBoth() {
        NewModelConfiguration config = new NewModelConfiguration("0-60", "1,2", "0-60", "1,2");
        assertThat(config.newModelEnabled(USER_ID), is(true));
        assertThat(config.useNewModel(USER_ID), is(true));
    }

    @Test
    public void testForUserIdInNone() {
        NewModelConfiguration config = new NewModelConfiguration("60-100", "2,3", "60-100", "2,3");
        assertThat(config.newModelEnabled(USER_ID), is(false));
        assertThat(config.useNewModel(USER_ID), is(false));
    }
}