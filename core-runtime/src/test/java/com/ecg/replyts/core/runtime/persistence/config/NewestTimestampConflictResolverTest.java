package com.ecg.replyts.core.runtime.persistence.config;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

/**
 * @author mhuttar
 */
@RunWith(MockitoJUnitRunner.class)
public class NewestTimestampConflictResolverTest {

    @Mock
    private Configurations obj1;

    @Mock
    private Configurations obj2;

    private List<Configurations> objs = new ArrayList<Configurations>();

    private NewestTimestampConflictResolver resolver = new NewestTimestampConflictResolver();

    @Before
    public void setUp() throws Exception {
        when(obj1.getTimestamp()).thenReturn(100l);
        when(obj2.getTimestamp()).thenReturn(200l);

    }

    @Test
    public void returnsNullIfNoVersion() throws Exception {
        assertNull(resolver.resolve(objs));
    }

    @Test
    public void picksOneIfOneAvailable() throws Exception {
        objs.add(obj2);
        assertEquals(obj2, resolver.resolve(objs));
    }

    @Test
    public void picksLatestIfMultiple() throws Exception {
        objs.add(obj1);
        objs.add(obj2);
        objs.add(obj1);
        assertEquals(obj2, resolver.resolve(objs));
    }
}
