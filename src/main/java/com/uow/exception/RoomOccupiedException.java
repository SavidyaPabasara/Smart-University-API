package com.uow.exception;

/**
 * Thrown when a client tries to delete a room that still has sensors assigned.
 * Mapped to HTTP 409 Conflict by RoomOccupiedMapper.
 */
public class RoomOccupiedException extends RuntimeException {
    public RoomOccupiedException(String message) {
        super(message);
    }
}
