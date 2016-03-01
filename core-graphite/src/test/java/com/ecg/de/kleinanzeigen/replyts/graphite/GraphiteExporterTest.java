package com.ecg.de.kleinanzeigen.replyts.graphite;

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.assertNotNull;

public class GraphiteExporterTest {
    @Test
    public void graphiteExporterCreated() throws Exception {
        new GraphiteExporter(true, "localhost", 80, 10, "rts2");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullHostname_exceptionThrown() throws Exception {
        new GraphiteExporter(true, null, 80, 10, "rts2");
    }

    @Test(expected = UnknownHostException.class)
    public void unresolvableHost_exceptionThrown() throws Exception {
        new GraphiteExporter(true, "nonexistenthostname-1234112341234.asdfasdf", 80, 10, "rts2");
    }

    @Test
    public void reportingDisabled_skipGraphiteInitialization() throws Exception {
        new GraphiteExporter(false, null, 80, 10, "rts2");
        // Mockito isn't powerful enough to test static class interactions to make sure that
        // a registry is never instantiated, so just make sure that no exception is thrown
        // due to a null hostname.
    }
}
