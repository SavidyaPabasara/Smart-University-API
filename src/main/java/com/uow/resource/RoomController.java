package com.uow.resource;

import com.uow.InMemoryStore;
import com.uow.exception.RoomOccupiedException;
import com.uow.model.Room;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Part 2 — Room Management
 *
 * GET    /api/v1/rooms            list every room
 * POST   /api/v1/rooms            create a new room
 * GET    /api/v1/rooms/{roomId}   fetch a specific room
 * DELETE /api/v1/rooms/{roomId}   remove a room (blocked if sensors are present)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomController {

    private final InMemoryStore store = InMemoryStore.getInstance();

    // ------------------------------------------------------------------
    // GET /api/v1/rooms
    // Returns the full list of all rooms currently in the system.
    // Returning complete objects avoids the N+1 problem that would arise
    // if only IDs were returned and clients had to fetch each room separately.
    // ------------------------------------------------------------------
    @GET
    public Response listRooms() {
        List<Room> all = new ArrayList<>(store.getRooms().values());
        return Response.ok(all).build();
    }

    // ------------------------------------------------------------------
    // POST /api/v1/rooms
    // ------------------------------------------------------------------
    @POST
    public Response addRoom(Room incoming, @Context UriInfo uriInfo) {
        if (incoming == null || isBlank(incoming.getId())) {
            return badRequest("Room 'id' field is required.");
        }
        if (isBlank(incoming.getName())) {
            return badRequest("Room 'name' field is required.");
        }
        if (store.getRooms().containsKey(incoming.getId())) {
            return conflict("A room with ID '" + incoming.getId() + "' already exists.");
        }

        store.getRooms().put(incoming.getId(), incoming);

        URI location = uriInfo.getAbsolutePathBuilder().path(incoming.getId()).build();
        return Response.created(location).entity(incoming).build();
    }

    // ------------------------------------------------------------------
    // GET /api/v1/rooms/{roomId}
    // ------------------------------------------------------------------
    @GET
    @Path("{roomId}")
    public Response fetchRoom(@PathParam("roomId") String roomId) {
        Room found = store.getRooms().get(roomId);
        if (found == null) {
            return notFound("No room found with ID: " + roomId);
        }
        return Response.ok(found).build();
    }

    // ------------------------------------------------------------------
    // DELETE /api/v1/rooms/{roomId}
    //
    // Business rule: a room that still has sensors attached cannot be deleted.
    // Doing so would orphan the sensor records — they would reference a
    // room ID that no longer exists, breaking referential integrity.
    //
    // Idempotency note: the first successful DELETE returns 204 No Content.
    // A second DELETE on the same ID returns 404 because the room is gone.
    // The server-side state (room absent) is identical either way, so the
    // operation is effectively idempotent in terms of outcome, even though
    // the response code differs on repeated calls.
    // ------------------------------------------------------------------
    @DELETE
    @Path("{roomId}")
    public Response removeRoom(@PathParam("roomId") String roomId) {
        Room target = store.getRooms().get(roomId);
        if (target == null) {
            return notFound("No room found with ID: " + roomId);
        }
        if (!target.getSensorIds().isEmpty()) {
            throw new RoomOccupiedException(
                    "Room '" + roomId + "' cannot be deleted. It still has "
                    + target.getSensorIds().size() + " sensor(s) assigned to it. "
                    + "Please remove all sensors before decommissioning this room."
            );
        }
        store.getRooms().remove(roomId);
        return Response.noContent().build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------
    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Response badRequest(String msg) {
        return Response.status(Response.Status.BAD_REQUEST).entity(errBody(msg)).build();
    }

    private Response conflict(String msg) {
        return Response.status(Response.Status.CONFLICT).entity(errBody(msg)).build();
    }

    private Response notFound(String msg) {
        return Response.status(Response.Status.NOT_FOUND).entity(errBody(msg)).build();
    }

    private Map<String, Object> errBody(String msg) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",  "error");
        body.put("message", msg);
        return body;
    }
}
