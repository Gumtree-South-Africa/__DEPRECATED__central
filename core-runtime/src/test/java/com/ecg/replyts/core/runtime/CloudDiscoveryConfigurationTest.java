package com.ecg.replyts.core.runtime;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class CloudDiscoveryConfigurationTest {

    @InjectMocks
    CloudDiscoveryConfiguration parentDiscoveryConfiguration;

    @Mock
    DiscoveryClient discoveryClient;

    @Test
    public void testServiceDiscoveryShouldDiscoverSharedServiceWhenItsTenantTagIsNotFound() {
        ReflectionTestUtils.setField(parentDiscoveryConfiguration, "tenant", "ebayk");
        Mockito.when(discoveryClient.getInstances(Mockito.eq("elasticsearch"))).thenReturn(Arrays.asList(
                new DefaultServiceInstance("elasticsearch", "host9", 9999, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde")),
                new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));
        Map<String, Object> m = parentDiscoveryConfiguration.discoverServices();

        Assert.assertNotNull("No service discovered", m.get("search.es.endpoints"));
        Assert.assertEquals("host1:1111", m.get("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldOnlyDiscoverServicesWithCorrectTenantTag() {
        ReflectionTestUtils.setField(parentDiscoveryConfiguration, "tenant", "ebayk");
        Mockito.when(discoveryClient.getInstances(Mockito.eq("elasticsearch"))).thenReturn(Arrays.asList(
                new DefaultServiceInstance("elasticsearch", "host8", 8888, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-ebayk", "tenant-ebayk")),
                new DefaultServiceInstance("elasticsearch", "host9", 9999, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde")),
                new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));
        Map<String, Object> m = parentDiscoveryConfiguration.discoverServices();

        Assert.assertNotNull("No service discovered", m.get("search.es.endpoints"));
        Assert.assertEquals("host8:8888", m.get("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldDiscoverSharedService() {
        ReflectionTestUtils.setField(parentDiscoveryConfiguration, "tenant", "ebayk");
        Mockito.when(discoveryClient.getInstances(Mockito.eq("elasticsearch"))).thenReturn(Collections.singletonList(
                new DefaultServiceInstance("elasticsearch", "host1", 9999, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));
        Map<String, Object> m = parentDiscoveryConfiguration.discoverServices();

        Assert.assertNotNull("No service discovered", m.get("search.es.endpoints"));
        Assert.assertEquals("host1:9999", m.get("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldDiscoverMultipleServices() {
        ReflectionTestUtils.setField(parentDiscoveryConfiguration, "tenant", "ebayk");
        Mockito.when(discoveryClient.getInstances(Mockito.eq("elasticsearch"))).thenReturn(Arrays.asList(
                new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
                new DefaultServiceInstance("elasticsearch", "host2", 2222, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
                new DefaultServiceInstance("elasticsearch", "host3", 3333, false, ImmutableMap.of("elasticsearch", "elasticsearch"))
        ));
        Map<String, Object> m = parentDiscoveryConfiguration.discoverServices();

        Assert.assertNotNull("No service discovered", m.get("search.es.endpoints"));
        Assert.assertEquals("host1:1111,host2:2222,host3:3333", m.get("search.es.endpoints"));
    }

    @Test
    public void testServiceDiscoveryShouldDiscoverMultipleTaggedServices() {
        ReflectionTestUtils.setField(parentDiscoveryConfiguration, "tenant", "ebayk");
        Mockito.when(discoveryClient.getInstances(Mockito.eq("elasticsearch"))).thenReturn(Arrays.asList(
                new DefaultServiceInstance("elasticsearch", "host1", 1111, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
                new DefaultServiceInstance("elasticsearch", "host2", 2222, false, ImmutableMap.of("elasticsearch", "elasticsearch")),
                new DefaultServiceInstance("elasticsearch", "host3", 3333, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-ebayk", "tenant-ebayk")),
                new DefaultServiceInstance("elasticsearch", "host4", 4444, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-ebayk", "tenant-ebayk")),
                new DefaultServiceInstance("elasticsearch", "host5", 5555, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde")),
                new DefaultServiceInstance("elasticsearch", "host6", 6666, false, ImmutableMap.of("elasticsearch", "elasticsearch", "tenant-mde", "tenant-mde"))
        ));
        Map<String, Object> m = parentDiscoveryConfiguration.discoverServices();

        Assert.assertNotNull("No service discovered", m.get("search.es.endpoints"));
        Assert.assertEquals("host3:3333,host4:4444", m.get("search.es.endpoints"));
    }

}