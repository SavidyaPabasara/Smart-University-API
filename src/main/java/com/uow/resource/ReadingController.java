package com.uow.resource;

import com.uow.InMemoryStore;
import com.uow.exception.SensorOfflineException;
import com.uow.model.Sensor;
import com.uow.model.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Part 4.2 — Historical Reading Management (Sub-Resource)
 *
 * This class is NOT registered in CampusApplication.getClasses().
 * JAX-RS never discovers it directly — instead, SensorController's
 * sub-resource locator method returns an instance of this class, and
 * JAX-RS then inspects it for @GET / @POST to finish routing.
 *
 * Effective URL patterns:
 *   GET  /api/v1/sensors/{sensorId}/readings    fetch reading history
 *   POST /api/v1/sensors/{sensorId}/readings    record a new reading
 *
 * Architectural benefit of this pattern:
 * All reading logic lives here. SensorController stays clean. Adding a new
 * reading endpoint (e.g., DELETE /readings/{id}) only touches this class.
 * A single monolithic controller with all nested paths becomes unmaintainable
 * as the API grows — sub-resource locators solve this by composition.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReadingController {

    private final String        sensorId;
    private final InMemoryStore store = InMemoryStore.getInstance();

    public ReadingController(String sensorId) {
        this.sensorId = sensorId;
    }

    // ------------------------------------------------------------------
    // GET /api/v1/sensors/{sensorId}/readings
    // ------------------------------------------------------------------
    @GET
    public Response getHistory() {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return notFound("No sensor found with ID: " + sensorId);
        }

        List<SensorReading> history = store.getReadingsForSensor(sensorId);
        return Response.ok(history).build();
    }

    // ------------------------------------------------------------------
    // POST /api/v1/sensors/{sensorId}/readings
    //
    // State constraint: sensors under MAINTENANCE cannot accept readings.
    // Throws SensorOfflineException → HTTP 403 Forbidden.
    //
    // Side effect: after storing the reading, the parent sensor's
    // currentValue is updated to reflect the latest measurement,
    // keeping the API's data consistent across all endpoints.
    // ------------------------------------------------------------------
    @POST
    public Response recordReading(SensorReading reading, @Context UriInfo uriInfo) {
        Sensor sensor = store.getSensors().get(sensorId);
        if (sensor == null) {
            return notFound("No sensor found with ID: " + sensorId);
        }

        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorOfflineException(
                    "Sensor '" + sensorId + "' is currently under MAINTENANCE. "
                    + "It cannot accept new readings until it is back ACTIVE."
            );
        }

        if (reading == null) {
            return Response.status(400).entity(errBody("Request body is required.")).build();
        }

        // Auto-assign ID and capture timestamp server-side
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        store.getReadingsForSensor(sensorId).add(reading);

        // Side effect: keep parent sensor's currentValue in sync
        sensor.setCurrentValue(reading.getValue());

        URI location = uriInfo.getAbsolutePathBuilder().path(reading.getId()).build();
        return Response.created(location).entity(reading).build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
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
