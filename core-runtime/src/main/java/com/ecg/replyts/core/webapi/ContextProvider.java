package com.ecg.replyts.core.webapi;

import org.eclipse.jetty.server.Handler;

/**
 * provides a context for the embedded webserver. The embedded webserver will try to launch the given context and upon
 * completion, will call the {@link ContextProvider}s {@link #test()} method to ensure that the context started up
 * successfully.
 *
 * @author mhuttar
 */
public interface ContextProvider {

    /**
     * @return a jetty handler representing a web context. Please note that this handler MAY NOT map itself to the root
     * path as this may clash with other contexts.
     */
    Handler createContext();

    /**
     * called when jetty started to ensure the context started up correctly. E.g. the Spring Dispatcher Servlet of that context is running and not causing issues.<br/>
     * If not running properly, this method is expected to throw a {@link RuntimeException}. <p>
     * A context that does not start successfully will interrupt the ReplyTS startup and cause ReplyTS to shutdown abnormally.
     */
    void test();

    String getContextPath();
}
