package com.ecg.replyts.core.webapi;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class SpringContextProvider implements ContextProvider {
    private String path;

    private XmlWebApplicationContext context;

    private ApplicationContext parentContext;

    private String[] contextLocations;

    /**
     * @param path             context path (e.g. /apiv2)
     * @param contextLocations path to spring context xml files for this spring webapp
     * @param parentContext    parent app context to be set to the {@link DispatcherServlet}'s context.
     */
    public SpringContextProvider(String path, String[] contextLocations, ApplicationContext parentContext) {
        this.path = path;
        this.contextLocations = contextLocations;
        this.parentContext = parentContext;

        if (parentContext instanceof WebApplicationContext)
            throw new IllegalArgumentException("Trying to create a web context within a web context");
    }

    @Override
    public Handler create() {
        context = new XmlWebApplicationContext();

        context.setConfigLocations(contextLocations);
        context.setParent(parentContext);

        ((ConfigurableEnvironment) parentContext.getEnvironment()).getPropertySources().forEach(source -> context.getEnvironment().getPropertySources().addFirst(source));

        DispatcherServlet dispatcherServlet = new DispatcherServlet(context);
        ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);

        // TODO: add proper error handling. We tried the thing below but the only effect we found was that no error page is returned now.

        /*

        ErrorPageErrorHandler eh = new ErrorPageErrorHandler();

        eh.addErrorPage(500, "/error500");
        eh.addErrorPage(404, "/error404");

        sch.setErrorHandler(eh);

        */

        contextHandler.setContextPath(path);
        contextHandler.addServlet(new ServletHolder(dispatcherServlet), "/*");

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
