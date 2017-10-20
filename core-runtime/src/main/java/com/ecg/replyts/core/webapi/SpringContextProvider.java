package com.ecg.replyts.core.webapi;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AbstractRefreshableWebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.DispatcherType;

import java.util.EnumSet;

public class SpringContextProvider implements ContextProvider {
    private String path;

    private AbstractRefreshableWebApplicationContext context;

    private ApplicationContext parentContext;

    /**
     * @param path             context path (e.g. /apiv2)
     * @param contextLocations path to spring context xml files for this spring webapp
     * @param parentContext    parent app context to be set to the {@link DispatcherServlet}'s context.
     */
    public SpringContextProvider(String path, String[] contextLocations, ApplicationContext parentContext) {
        this.path = path;
        this.parentContext = parentContext;

        if (parentContext instanceof WebApplicationContext) {
            throw new IllegalArgumentException("Trying to create a web context within a web context");
        }

        XmlWebApplicationContext context = new XmlWebApplicationContext();

        context.setParent(parentContext);
        context.setConfigLocations(contextLocations);

        this.context = context;
    }

    /**
     * @param path               context path (e.g. /apiv2)
     * @param configurationClass path to spring context xml files for this spring webapp
     * @param parentContext      parent app context to be set to the {@link DispatcherServlet}'s context.
     */
    public SpringContextProvider(String path, Class configurationClass, ApplicationContext parentContext) {
        this.path = path;
        this.parentContext = parentContext;

        if (parentContext instanceof WebApplicationContext) {
            throw new IllegalArgumentException("Trying to create a web context within a web context");
        }

        AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

        context.setParent(parentContext);
        context.register(configurationClass);

        this.context = context;
    }

    @Override
    public Handler create() {
        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);

        contextHandler.setContextPath(path);
        contextHandler.addFilter(CorrelationIdFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        contextHandler.addServlet(new ServletHolder(dispatcherServlet), "/*");
        contextHandler.addEventListener(new ContextLoaderListener(context));

        context.setServletContext(contextHandler.getServletContext());
        context.refresh();

        return contextHandler;
    }

    @Override
    public void test() {
        if (context == null)
            throw new IllegalStateException("Spring context is being tested but hasn't been initialized yet");

        try {
            if (!context.isRunning())
                throw new IllegalStateException("Spring context is not running");
        } catch (Exception e) {
            throw new RuntimeException("Spring context not running", e);
        }
    }

    @Override
    public String getPath() {
        return path;
    }
}
