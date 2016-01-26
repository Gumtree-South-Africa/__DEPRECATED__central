package com.ecg.replyts.core.webapi;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.Lifecycle;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;


/**
 * Simple Spring Context Provider. Creates a jetty context and maps a {@link DispatcherServlet} to /* on this context subpath.
 *
 * @author mhuttar
 */
public class SpringContextProvider implements ContextProvider {

    private XmlWebApplicationContext subC;
    private final ApplicationContext parentApplicationContext;
    private final String path;
    private final String[] contextConfigLocations;

    /**
     * @param path                     context path (e.g. /apiv2)
     * @param contextConfigLocations   path to spring context xml files for this spring webapp
     * @param parentApplicationContext parent app context to be set to the {@link DispatcherServlet}'s context.
     */
    public SpringContextProvider(String path, String[] contextConfigLocations, ApplicationContext parentApplicationContext) {
        this.path = path;
        this.contextConfigLocations = contextConfigLocations.clone();
        this.parentApplicationContext = parentApplicationContext;

    }

    @Override
    public Handler createContext() {

        subC = new XmlWebApplicationContext();
        subC.setParent(parentApplicationContext);
        subC.setConfigLocations(contextConfigLocations);
        DispatcherServlet dispatcherServlet = new DispatcherServlet(subC);


        ServletContextHandler sch = new ServletContextHandler(ServletContextHandler.NO_SESSIONS | ServletContextHandler.NO_SECURITY);
        // TODO: add proper error handling. We tried the thing below but the only effect we found was that no error page is returned now.
        /* ErrorPageErrorHandler eh = new ErrorPageErrorHandler();
        eh.addErrorPage(500, "/error500");
        eh.addErrorPage(404, "/error404");
        sch.setErrorHandler(eh); */
        sch.setContextPath(path);
        sch.addServlet(new ServletHolder(dispatcherServlet), "/*");
        return sch;
    }

    @Override
    public void test() {
        expectContextRunning(subC);
    }

    private static void expectContextRunning(Lifecycle c) {
        try {
            if (!c.isRunning()) {
                throw new IllegalStateException("Spring Context is not running");
            }
        } catch (Exception e) {
            throw new RuntimeException("Spring Context not running", e);
        }
    }

    @Override
    public String getContextPath() {
        return path;
    }
}
