package com.uow.mapper;

import com.uow.exception.SensorOfflineException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps SensorOfflineException to HTTP 403 Forbidden.
 */
@Provider
public class SensorOfflineMapper implements ExceptionMapper<SensorOfflineException> {

    @Override
    public Response toResponse(SensorOfflineException ex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status",  "error");
        payload.put("code",    403);
        payload.put("error",   "Sensor Unavailable");
        payload.put("message", ex.getMessage());

        return Response.status(403)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .build();
    }
}
