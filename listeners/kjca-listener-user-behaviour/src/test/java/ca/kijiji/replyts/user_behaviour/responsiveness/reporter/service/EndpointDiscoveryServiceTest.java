package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.service;

import ca.kijiji.discovery.*;
import com.ecg.replyts.core.runtime.retry.RetryException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EndpointDiscoveryServiceTest {

    private EndpointDiscoveryService objectUnderTest;

    @Mock
    private ServiceDirectory serviceDirectoryMock;

    @Mock
    private LookupRequest lookupRequestMock;

    @Before
    public void setUp() {
        objectUnderTest = new EndpointDiscoveryService(serviceDirectoryMock, lookupRequestMock);
    }

    @Test(expected = RetryException.class)
    @SuppressWarnings("unchecked")
    public void whenLookupFailed_shouldThrowException() throws DiscoveryFailedException, RetryException {
        when(serviceDirectoryMock.lookup(any(SelectionStrategy.class), any(LookupRequest.class))).thenThrow(Exception.class);

        objectUnderTest.discoverEndpoints();
    }

    @Test
    public void whenNoEndpoints_shouldReturnEmptyCollection() throws DiscoveryFailedException, RetryException {
        LookupResult lookupResultMock = mock(LookupResult.class);

        when(serviceDirectoryMock.lookup(any(SelectionStrategy.class), any(LookupRequest.class))).thenReturn(lookupResultMock);
        when(lookupResultMock.all()).thenReturn(Collections.emptyList());

        assertThat(objectUnderTest.discoverEndpoints()).isEmpty();
    }

    @Test
    public void whenEndpointsFound_shouldReturnEndpoints() throws DiscoveryFailedException, RetryException {
        LookupResult lookupResultMock = mock(LookupResult.class);
        ServiceEndpoint expected = new ServiceEndpoint("address", 8080);

        when(serviceDirectoryMock.lookup(any(SelectionStrategy.class), any(LookupRequest.class))).thenReturn(lookupResultMock);
        when(lookupResultMock.all()).thenReturn(Collections.singletonList(expected));

        List<ServiceEndpoint> actual = objectUnderTest.discoverEndpoints();
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).isEqualTo(expected);
    }
}
