package com.ecg.gumtree.comaas.common.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class RequestResponseFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestResponseFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) {
        LOG.info("Path: " + requestContext.getRequest().getMethod() + " " + requestContext.getUriInfo().getAbsolutePath());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        LOG.info("Status" + responseContext.getStatusInfo() + ", Entity: " + responseContext.getEntity());
    }
}