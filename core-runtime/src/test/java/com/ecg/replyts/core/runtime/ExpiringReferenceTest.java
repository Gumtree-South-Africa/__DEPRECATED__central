package com.ecg.replyts.core.runtime;

import com.ecg.replyts.core.api.util.Clock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author mhuttar
 */
@RunWith(MockitoJUnitRunner.class)
public class ExpiringReferenceTest {

    @Mock
    Clock c;

    @Mock
    private Date date;

    private ExpiringReference ref;

    @Before
    public void setUp() throws Exception {
        when(c.now()).thenReturn(date);
        when(date.getTime()).thenReturn(0l);
        ref = new ExpiringReference(Boolean.FALSE, 5000, c);
    }

    @Test
    public void defaultsToExpiredValue() throws Exception {
        assertEquals(Boolean.FALSE, ref.get());

    }

    @Test
    public void keepsValueUntilExpiry() throws Exception {
        ref.set(Boolean.TRUE);
        when(date.getTime()).thenReturn(4999l);
        assertEquals(Boolean.TRUE, ref.get());
    }

    @Test
    public void returnsDefaultValueAfterExpiry() throws Exception {
        ref.set(Boolean.TRUE);
        when(date.getTime()).thenReturn(10000l);
        assertEquals(Boolean.FALSE, ref.get());
    }

    @Test
    public void reportsNotExpiredValueCorrect() {
        ref.set(Boolean.TRUE);
        when(date.getTime()).thenReturn(1543l);
        assertEquals("Valid: true (set 1.543 secs ago, expires in 3.457 secs)", ref.report());
    }

    @Test
    public void reportsExpiredValueCorrect() {
        ref.set(Boolean.TRUE);
        when(date.getTime()).thenReturn(6543l);
        assertEquals("Expired, Now: false (before: true; set 6.543 secs ago, expired 1.543 secs ago)", ref.report());
    }
}
