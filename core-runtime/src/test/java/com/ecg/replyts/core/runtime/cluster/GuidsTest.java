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
    private Guids.JvmIdentifier pi;

    @Mock
    private Date date;

    private Guids guids;

    @Before
    public void setUp() throws Exception {
        when(clock.now()).thenReturn(date);
        when(date.getTime()).thenReturn(10l); // b in base30

        when(pi.getId()).thenReturn("pid");

        guids = new Guids(10l, clock, pi);
    }

    @Test
    public void generateIds() throws Exception {
        // 11:xx
        assertThat(guids.nextGuid()).isEqualTo("c:pid:b");
        // 12:xx
        assertThat(guids.nextGuid()).isEqualTo("d:pid:b");
        // 13:xx
        assertThat(guids.nextGuid()).isEqualTo("f:pid:b");

    }

    @Test
    public void idsCounterComponentHandlesLongRollover() {
        final Guids guids = new Guids(Long.MAX_VALUE, clock, pi);
        assertThat(guids.nextGuid()).isEqualTo("kbmttcd1hd208:pid:b");
    }

    @Test
    public void jvmIdentifierSameResultForCallingItTwice() {
        assertThat(new Guids.JvmIdentifier().getId()).isEqualTo(new Guids.JvmIdentifier().getId());
    }
}