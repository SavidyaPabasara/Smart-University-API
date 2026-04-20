package com.uow.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Part 1 — Discovery Endpoint
 *
 * GET /api/v1/
 *
 * Returns API metadata including version, contact details, and navigational
 * links to all primary resource collections (HATEOAS principle).
 *
 * HATEOAS benefit: clients discover available resources from the response
 * itself rather than relying on hardcoded URLs or external documentation.
 * If a URL changes server-side, clients following embedded links adapt
 * automatically without any code changes on their end.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class ApiRootResource {

    @GET
    public Response discover() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("api",         "Smart Campus Sensor & Room Management API");
        info.put("version",     "v1");
        info.put("contact",     "admin@smartcampus.ac.uk");
        info.put("description", "RESTful API for managing campus rooms and IoT sensors.");

        Map<String, String> links = new LinkedHashMap<>();
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        info.put("resources", links);

        return Response.ok(info).build();
    }
}
