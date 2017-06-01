package com.ecg.gumtree.comaas.filter.volume;

import com.google.common.collect.ImmutableList;
import com.gumtree.filters.comaas.config.Result;
import com.gumtree.filters.comaas.config.State;
import com.gumtree.filters.comaas.config.VelocityFilterConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@Import(EventStreamProcessor.class)
public class EventStreamProcessorTest {
    @Autowired
    private EventStreamProcessor eventStreamProcessor;

    private static final String INSTANCE_ID = "test-instance";
    private static boolean isSetup = false;

    @Before
    public void setup() {
        if (!isSetup) {
            eventStreamProcessor.register(INSTANCE_ID, filterConfig());
            isSetup = true;
        }
    }

    @Test
    public void canLogSingleMailReceivedEventAndSearchInTimeWindow() throws Exception {
        eventStreamProcessor.mailReceivedFrom("event1b");
        long count = eventStreamProcessor.count("event1b", INSTANCE_ID);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    public void canLogMultipleMailReceivedEventsAndSearchInTimeWindowForSpecificValue() {
        eventStreamProcessor.mailReceivedFrom("event1a");
        eventStreamProcessor.mailReceivedFrom("event2a");
        eventStreamProcessor.mailReceivedFrom("event2a");
        eventStreamProcessor.mailReceivedFrom("event3a");

        assertThat(eventStreamProcessor.count("event1a", INSTANCE_ID)).isEqualTo(1L);
        assertThat(eventStreamProcessor.count("event2a", INSTANCE_ID)).isEqualTo(2L);
        assertThat(eventStreamProcessor.count("event3a", INSTANCE_ID)).isEqualTo(1L);
    }

    @Test
    public void testRename() throws Exception {
        assertNameChange("no_change", "no_change");
        assertNameChange("some_change", "some+change");
        assertNameChange("some_change", "some-change");
        assertNameChange("some_change", "some change");
        assertNameChange("some___change", "some % change");
        assertNameChange("some____change_", "some + (change)");
    }

    private void assertNameChange(String expected, String input) {
        assertThat(eventStreamProcessor.windowName(input)).isEqualTo(EventStreamProcessor.VOLUME_NAME_PREFIX + expected);
    }

    private VelocityFilterConfig filterConfig() {
        return new VelocityFilterConfig.Builder(State.ENABLED, 1, Result.HOLD)
                .withSeconds(100)
                .withExceeding(true)
                .withExemptedCategories(ImmutableList.of(1234L, 4321L))
                .withMessages(1)
                .withWhitelistSeconds(3600)
                .withFilterField(VelocityFilterConfig.FilterField.EMAIL)
                .withMessageState(VelocityFilterConfig.MessageState.FILTERABLE)
                .build();
    }
}