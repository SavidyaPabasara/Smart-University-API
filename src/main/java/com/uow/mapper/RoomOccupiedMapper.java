package com.uow.mapper;

import com.uow.exception.RoomOccupiedException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps RoomOccupiedException to HTTP 409 Conflict with a structured JSON body.
 */
@Provider
public class RoomOccupiedMapper implements ExceptionMapper<RoomOccupiedException> {

    @Override
    public Response toResponse(RoomOccupiedException ex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status",  "error");
        payload.put("code",    409);
        payload.put("error",   "Room Conflict");
        payload.put("message", ex.getMessage());

        return Response.status(409)
                .type(MediaType.APPLICATION_JSON)
                .entity(payload)
                .build();
    }
}
