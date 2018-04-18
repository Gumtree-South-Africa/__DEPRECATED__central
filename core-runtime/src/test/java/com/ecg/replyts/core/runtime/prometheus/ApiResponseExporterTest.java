package com.ecg.replyts.core.runtime.prometheus;

import io.prometheus.client.Collector.MetricFamilySamples;
import io.prometheus.client.Collector.MetricFamilySamples.Sample;
import io.prometheus.client.CollectorRegistry;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Enumeration;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiResponseExporterTest {

    private static Server SERVER;
    private static String BASE_URL;
    private static CloseableHttpClient HTTP_CLIENT;
    private static ApiResponseExporter apiResponseExporter;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        CollectorRegistry.defaultRegistry.clear();

        // Create Server
        SERVER = new Server(0);
        ServletContextHandler context = new ServletContextHandler();
        ServletHolder defaultServ = new ServletHolder("default", DefaultServlet.class);
        defaultServ.setInitParameter("resourceBase",System.getProperty("user.dir"));
        defaultServ.setInitParameter("dirAllowed","true");
        context.addServlet(defaultServ,"/");

        apiResponseExporter = new ApiResponseExporter();
        apiResponseExporter.setHandler(context);
        SERVER.setHandler(apiResponseExporter);
        SERVER.start();

        Connector[] connectors = SERVER.getConnectors();
        NetworkConnector connector = (NetworkConnector) connectors[0];
        BASE_URL = "http://localhost:" + connector.getLocalPort() + "/";

        HTTP_CLIENT = HttpClientBuilder.create().build();
    }

    @AfterClass
    public static void afterClass()
    {
        try {
            HTTP_CLIENT.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            SERVER.stop();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        CollectorRegistry.defaultRegistry.clear();
    }

    @Test
    public void rootContextPath_shouldNotRegister() throws Exception
    {
        CollectorRegistry.defaultRegistry.clear();
        apiResponseExporter.doStart();

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(new HttpGet(BASE_URL))) {
            Assertions.assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
        }

        Enumeration<MetricFamilySamples> metricFamilySamples = CollectorRegistry.defaultRegistry.metricFamilySamples();
        while (metricFamilySamples.hasMoreElements()) {
            assertThat(metricFamilySamples.nextElement().samples).isEmpty();
        }
    }

    @Test
    public void contextPath_shouldbeRegistered() throws Exception
    {
        CollectorRegistry.defaultRegistry.clear();
        apiResponseExporter.doStart();

        try (CloseableHttpResponse response = HTTP_CLIENT.execute(new HttpGet(BASE_URL + "screeningv2/test"))) {
            Assertions.assertThat(response.getStatusLine().getStatusCode()).isEqualTo(404);
        }

        Enumeration<MetricFamilySamples> metricFamilySamples = CollectorRegistry.defaultRegistry.metricFamilySamples();
        while (metricFamilySamples.hasMoreElements()) {
            for (Sample sample : metricFamilySamples.nextElement().samples) {
                assertThat(sample.labelValues.get(sample.labelNames.indexOf("context_path"))).isEqualTo("screeningv2");
            }
        }
    }
}
