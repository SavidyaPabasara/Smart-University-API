package com.uow.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global safety net — intercepts every Throwable not caught by a more specific mapper.
 *
 * Two cases are handled:
 *
 *  1. WebApplicationException (JAX-RS built-ins such as 404 NotFoundException,
 *     405 NotAllowedException, 415 NotSupportedException):
 *     These already carry the correct HTTP status code. We extract it and return
 *     a clean JSON body instead of an HTML error page.
 *
 *  2. All other Throwables (NullPointerException, etc.):
 *     The full stack trace is logged server-side for debugging, and a generic
 *     HTTP 500 message is returned to the client.
 *
 * Security rationale: exposing raw stack traces reveals internal class names,
 * library versions, and code paths that attackers can exploit. This mapper
 * ensures the client never sees them.
 */
@Provider
public class CatchAllMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(CatchAllMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {

        if (ex instanceof WebApplicationException) {
            int httpCode = ((WebApplicationException) ex).getResponse().getStatus();
            Response.Status statusEnum = Response.Status.fromStatusCode(httpCode);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status",  "error");
            body.put("code",    httpCode);
            body.put("error",   statusEnum != null ? statusEnum.getReasonPhrase() : "HTTP Error");
            body.put("message", "The requested resource or method was not found.");

            return Response.status(httpCode)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(body)
                    .build();
        }

        LOG.log(Level.SEVERE, "Unexpected server error: " + ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("code",    500);
        body.put("error",   "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact the system administrator.");

        return Response.status(500)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
