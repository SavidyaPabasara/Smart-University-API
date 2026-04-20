package com.uow.model;

/**
 * A single timestamped measurement captured by a sensor.
 */
public class SensorReading {

    private String id;          // auto-generated UUID
    private long   timestamp;   // epoch milliseconds
    private double value;       // the recorded measurement

    public SensorReading() {}

    public SensorReading(String id, long timestamp, double value) {
        this.id        = id;
        this.timestamp = timestamp;
        this.value     = value;
    }

    public String getId()                  { return id; }
    public void   setId(String id)         { this.id = id; }

    public long   getTimestamp()           { return timestamp; }
    public void   setTimestamp(long ts)    { this.timestamp = ts; }

    public double getValue()               { return value; }
    public void   setValue(double value)   { this.value = value; }
}
