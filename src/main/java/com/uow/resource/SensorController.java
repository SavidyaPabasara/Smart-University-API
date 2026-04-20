package com.uow.resource;

import com.uow.InMemoryStore;
import com.uow.exception.InvalidReferenceException;
import com.uow.model.Room;
import com.uow.model.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Part 3 — Sensor Operations
 *
 * GET  /api/v1/sensors              list all sensors (optional ?type= filter)
 * POST /api/v1/sensors              register a new sensor
 * GET  /api/v1/sensors/{sensorId}   fetch a single sensor
 *
 * Part 4 — Sub-Resource Locator
 * ANY  /api/v1/sensors/{sensorId}/readings  delegates to ReadingController
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorController {

    private final InMemoryStore store = InMemoryStore.getInstance();

    // ------------------------------------------------------------------
    // GET /api/v1/sensors
    // Optional query param: ?type=CO2
    //
    // @QueryParam is preferred over a path segment (/sensors/type/CO2) because:
    //  - Query params express filtering/searching, not resource identity
    //  - ?type= is optional naturally; a path segment would need a separate route
    //  - Multiple filters compose cleanly: ?type=CO2&status=ACTIVE
    //  - REST convention: paths identify resources, query params narrow results
    // ------------------------------------------------------------------
    @GET
    public Response listSensors(@QueryParam("type") String type) {
        List<Sensor> result = new ArrayList<>(store.getSensors().values());

        if (type != null && !type.trim().isEmpty()) {
            result = result.stream()
                    .filter(s -> s.getType() != null && s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    // ------------------------------------------------------------------
    // POST /api/v1/sensors
    //
    // Validates that the roomId in the request body refers to a real room.
    // If not, throws InvalidReferenceException → HTTP 422.
    //
    // @Consumes(APPLICATION_JSON): if a client sends text/plain or XML,
    // JAX-RS rejects the request immediately with HTTP 415 Unsupported Media Type
    // before the method body is ever reached.
    // ------------------------------------------------------------------
    @POST
    public Response registerSensor(Sensor incoming, @Context UriInfo uriInfo) {
        if (incoming == null || isBlank(incoming.getId())) {
            return badRequest("Sensor 'id' field is required.");
        }
        if (isBlank(incoming.getRoomId())) {
            return badRequest("Sensor 'roomId' field is required.");
        }

        // Referential integrity: roomId must point to an existing room
        Room linkedRoom = store.getRooms().get(incoming.getRoomId());
        if (linkedRoom == null) {
            throw new InvalidReferenceException(
                    "Cannot register sensor: room with ID '" + incoming.getRoomId()
                    + "' does not exist in the system."
            );
        }

        if (store.getSensors().containsKey(incoming.getId())) {
            return conflict("A sensor with ID '" + incoming.getId() + "' already exists.");
        }

        if (isBlank(incoming.getStatus())) {
            incoming.setStatus("ACTIVE");
        }

        store.getSensors().put(incoming.getId(), incoming);

        // Keep the room's sensorIds list in sync
        linkedRoom.getSensorIds().add(incoming.getId());

        URI location = uriInfo.getAbsolutePathBuilder().path(incoming.getId()).build();
        return Response.created(location).entity(incoming).build();
    }

    // ------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}
    // ------------------------------------------------------------------
    @GET
    @Path("{sensorId}")
    public Response fetchSensor(@PathParam("sensorId") String sensorId) {
        Sensor found = store.getSensors().get(sensorId);
        if (found == null) {
            return notFound("No sensor found with ID: " + sensorId);
        }
        return Response.ok(found).build();
    }

    // ------------------------------------------------------------------
    // Part 4.1 — Sub-Resource Locator
    //
    // No HTTP verb annotation here. JAX-RS does NOT handle the request
    // in this method — it instantiates ReadingController and passes control
    // to it. ReadingController then handles GET and POST on the readings path.
    //
    // This pattern keeps SensorController focused on sensor CRUD and
    // delegates reading history to a dedicated class, avoiding a bloated
    // controller with dozens of unrelated methods.
    // ------------------------------------------------------------------
    @Path("{sensorId}/readings")
    public ReadingController getReadingController(@PathParam("sensorId") String sensorId) {
        return new ReadingController(sensorId);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Response badRequest(String msg) {
        return Response.status(400).entity(errBody(msg)).build();
    }

    private Response conflict(String msg) {
        return Response.status(409).entity(errBody(msg)).build();
    }

    private Response notFound(String msg) {
        return Response.status(404).entity(errBody(msg)).build();
    }

    private Map<String, Object> errBody(String msg) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("message", msg);
        return body;
    }
}
