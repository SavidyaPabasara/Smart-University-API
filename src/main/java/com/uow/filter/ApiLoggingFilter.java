package com.uow.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Dual-purpose JAX-RS filter that logs every incoming request and every
 * outgoing response in one place.
 *
 * Implementing both ContainerRequestFilter and ContainerResponseFilter in a
 * single class is a cross-cutting concern pattern — the filter wraps the entire
 * request/response lifecycle without touching any resource method.
 *
 * Why use filters instead of Logger.info() in every resource method?
 *  - DRY: one class covers all endpoints automatically.
 *  - Consistency: no developer can forget to add logging to a new endpoint.
 *  - Separation: resource classes stay focused on business logic only.
 *  - Flexibility: logging can be changed or disabled by modifying this one class.
 */
@Provider
public class ApiLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(ApiLoggingFilter.class.getName());

    /** Runs before the request reaches a resource method. */
    @Override
    public void filter(ContainerRequestContext req) throws IOException {
        LOG.info(String.format(
                "--> Incoming Request  | Method: %-6s | URI: %s",
                req.getMethod(),
                req.getUriInfo().getRequestUri()
        ));
    }

    /** Runs after the resource method has produced a response. */
    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) throws IOException {
        LOG.info(String.format(
                "<-- Outgoing Response | Status: %-3d | URI: %s",
                res.getStatus(),
                req.getUriInfo().getRequestUri()
        ));
    }
}
