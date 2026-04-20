package com.uow.exception;

/**
 * Thrown when a reading is posted to a sensor whose status is MAINTENANCE.
 * Mapped to HTTP 403 Forbidden by SensorOfflineMapper.
 */
public class SensorOfflineException extends RuntimeException {
    public SensorOfflineException(String message) {
        super(message);
    }
}
