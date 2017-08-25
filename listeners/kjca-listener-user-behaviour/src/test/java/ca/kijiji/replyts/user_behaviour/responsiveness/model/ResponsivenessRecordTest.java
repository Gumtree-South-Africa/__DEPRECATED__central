package ca.kijiji.replyts.user_behaviour.responsiveness.model;

import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class ResponsivenessRecordTest {

    private static final Instant TIME_NOW = Instant.now();

    @Test
    public void toCsvLine_whenAllFieldsPresent_shouldReturnProperResult() {
        String actual = new ResponsivenessRecord(1, 1, "1", "1", 1, TIME_NOW)
                .toCsvLine();
        assertThat(actual).isEqualTo("1,1,1,1,1," + TIME_NOW.toEpochMilli());
    }

    @Test
    public void toCsvLine_whenAllFieldsNotPresent_shouldReturnProperResult() {
        String actual = new ResponsivenessRecord(0, 0, null, null, 0, null)
                .toCsvLine();
        assertThat(actual).isEqualTo("0,0,null,null,0,null");
    }
}
