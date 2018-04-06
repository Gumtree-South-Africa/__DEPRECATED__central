package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.comaas.core.filter.belenblockeduser.CompoundUserStateResolver;
import com.ecg.comaas.core.filter.belenblockeduser.ExternalUserTnsStateResolver;
import com.ecg.comaas.core.filter.belenblockeduser.UserDataStateResolver;
import com.ecg.comaas.core.filter.belenblockeduser.UserState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompoundUserStateResolverTest {

    @Mock
    private UserDataStateResolver userDataStateResolver;

    @Mock
    private ExternalUserTnsStateResolver externalUserTnsStateResolver;

    private CompoundUserStateResolver resolver;

    @Before
    public void setUp() {
        resolver = new CompoundUserStateResolver(userDataStateResolver, externalUserTnsStateResolver);
    }

    @Test
    public void usesUserDataStateIfNotUndecided() {
        when(userDataStateResolver.resolve(anyString())).thenReturn(UserState.ACTIVE);
        when(externalUserTnsStateResolver.resolve(anyString())).thenReturn(UserState.BLOCKED);
        assertEquals(UserState.ACTIVE, resolver.resolve("oo"));


    }

    @Test
    public void usesExternalUserStateIfUndecidedInUserdata() {
        when(userDataStateResolver.resolve(anyString())).thenReturn(UserState.UNDECIDED);
        when(externalUserTnsStateResolver.resolve(anyString())).thenReturn(UserState.BLOCKED);

        assertEquals(UserState.BLOCKED, resolver.resolve("foo"));
    }
}
