package com.ecg.de.kleinanzeigen.replyts.belen.blockeduser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExternalUserTnsStateResolverTest {

    @Mock
    private JdbcTemplate tpl;

    private ExternalUserTnsStateResolver stateResolver;

    private List<String> result = newArrayList();

    @Before
    public void setUp() {
        stateResolver = new ExternalUserTnsStateResolver(tpl);
        when(tpl.query(anyString(), any(RowMapper.class), any(String[].class))).thenReturn(result);
    }

    @Test
    public void returnsActiveWhenNotFound() {
        assertEquals(UserState.ACTIVE, stateResolver.resolve("foo@bar.com"));
    }

    @Test
    public void returnsActiveWhenFound() {
        result.add("ACTIVE");
        assertEquals(UserState.ACTIVE, stateResolver.resolve("foo@bar.com"));
    }

    @Test
    public void returnsBlockedWhenFound() {
        result.add("BLOCKED");
        assertEquals(UserState.BLOCKED, stateResolver.resolve("foo@bar.com"));
    }

    @Test
    public void returnsActiveOnOtherStates() {
        result.add("FOOOBAR");
        assertEquals(UserState.ACTIVE, stateResolver.resolve("foo@bar.com"));
    }
}
