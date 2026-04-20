package com.uow.mapper;

import com.uow.exception.InvalidReferenceException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps InvalidReferenceException to HTTP 422 Unprocessable Entity.
 *
 * Why 422 and not 404?
 * 404 means the requested URL was not found — but the URL is correct here.
 * The payload is syntactically valid JSON; the problem is that a value inside it
 * (the roomId field) references a resource that does not exist. HTTP 422 signals
 * exactly this: "I understood your request, but the content is semantically wrong."
 * A 404 would mislead clients into thinking they hit the wrong endpoint.
 */
@Provider
public class InvalidReferenceMapper implements ExceptionMapper<InvalidReferenceException> {

    @Override
    public Response toResponse(InvalidReferenceException ex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status",  "error");
        payload.put("code",    422);
        payload.put("error",   "Unprocessable Entity");
        payload.put("message", ex.getMessage());

        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .build();
    }
}
