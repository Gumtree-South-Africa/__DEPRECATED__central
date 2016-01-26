package com.ecg.replyts.core.runtime.cluster;

import com.ecg.replyts.core.api.util.Clock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GuidsTest {

    @Mock
    private Clock clock;

    @Mock
    private JvmIdentifier pi;

    @Mock
    private Date date;

    private Guids guids;

    @Before
    public void setUp() throws Exception {
        when(clock.now()).thenReturn(date);
        when(date.getTime()).thenReturn(10l); // a in 36digits

        when(pi.getId()).thenReturn("pid");

        guids = new Guids(10l, clock, pi);
    }


    @Test
    public void generateIds() throws Exception {
        // 11:xx
        assertThat(guids.nextGuid()).isEqualTo("b:pid:a");
        // 12:xx
        assertThat(guids.nextGuid()).isEqualTo("c:pid:a");
        // 13:xx
        assertThat(guids.nextGuid()).isEqualTo("d:pid:a");

    }
}
