package com.ecg.replyts.core.runtime.persistence.mail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = HybridHeldMailRepositoryTest.TestContext.class)
public class HybridHeldMailRepositoryTest {
    @MockBean
    private CassandraHeldMailRepository cassandraHeldMailRepository;

    @MockBean
    private RiakHeldMailRepository riakHeldMailRepository;

    @Autowired
    private HybridHeldMailRepository hybridHeldMailRepository;

    @Test
    public void testReadAlreadyMigrated() {
        when(cassandraHeldMailRepository.read("123")).thenReturn(new byte[] { 1, 2, 3 });

        byte[] content = hybridHeldMailRepository.read("123");

        assertEquals("Returned content contains three bytes", 3, content.length);
        verify(riakHeldMailRepository, never()).read(anyString());
        verify(cassandraHeldMailRepository, never()).write(anyString(), any(byte[].class));
    }

    @Test
    public void testReadMigration() {
        when(cassandraHeldMailRepository.read("123")).thenThrow(RuntimeException.class);
        when(riakHeldMailRepository.read("123")).thenReturn(new byte[] { 4, 5 });

        byte[] content = hybridHeldMailRepository.read("123");

        assertEquals("Returned content contains two bytes", 2, content.length);
        verify(riakHeldMailRepository, times(1)).read("123");
        verify(cassandraHeldMailRepository, times(1)).write(eq("123"), any(byte[].class));
    }

    @Test(expected = RuntimeException.class)
    public void testNowhereFound() {
        when(cassandraHeldMailRepository.read("123")).thenThrow(RuntimeException.class);
        when(riakHeldMailRepository.read("123")).thenThrow(RuntimeException.class);

        hybridHeldMailRepository.read("123");
    }

    @Test
    public void testWrite() {
        hybridHeldMailRepository.write("123", new byte[] { });

        verify(cassandraHeldMailRepository, times(1)).write(eq("123"), any(byte[].class));
        verify(riakHeldMailRepository, never()).write(anyString(), any(byte[].class));
    }

    @Test
    public void testRemove() {
        hybridHeldMailRepository.remove("123");

        verify(cassandraHeldMailRepository, times(1)).remove("123");
        verify(riakHeldMailRepository, never()).remove(anyString());
    }

    @Configuration
    static class TestContext {
        @Bean
        public HybridHeldMailRepository hybridHeldMailRepository(CassandraHeldMailRepository cassandraHeldMailRepository, RiakHeldMailRepository riakHeldMailRepository) {
            return new HybridHeldMailRepository(cassandraHeldMailRepository, riakHeldMailRepository);
        }
    }
}
