package com.uow;

import com.uow.model.Room;
import com.uow.model.Sensor;
import com.uow.model.SensorReading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory singleton that acts as the application's data store.
 *
 * Why a singleton?
 * JAX-RS instantiates resource classes once per request, so instance fields on
 * those classes are request-scoped and cannot hold shared data. By keeping all
 * data here — in a single object that lives for the lifetime of the JVM — every
 * resource instance reads and writes the same collections regardless of which
 * request created them.
 *
 * Why ConcurrentHashMap?
 * Multiple HTTP requests can arrive simultaneously. A plain HashMap is not
 * thread-safe and can corrupt its internal state under concurrent writes.
 * ConcurrentHashMap partitions its buckets so reads and writes from different
 * threads do not block each other unnecessarily, preventing race conditions
 * without requiring synchronized blocks around every operation.
 *
 * Why Collections.synchronizedList for readings?
 * Reading lists are grown via add(), which is not atomic on ArrayList.
 * wrapping with synchronizedList() ensures each add() is thread-safe.
 */
public class InMemoryStore {

    private static final InMemoryStore INSTANCE = new InMemoryStore();

    private final Map<String, Room>                    roomTable    = new ConcurrentHashMap<>();
    private final Map<String, Sensor>                  sensorTable  = new ConcurrentHashMap<>();
    private final Map<String, List<SensorReading>>     readingTable = new ConcurrentHashMap<>();

    private InMemoryStore() {}

    public static InMemoryStore getInstance() {
        return INSTANCE;
    }

    /** Returns the live room map — callers read and write directly. */
    public Map<String, Room> getRooms() {
        return roomTable;
    }

    /** Returns the live sensor map — callers read and write directly. */
    public Map<String, Sensor> getSensors() {
        return sensorTable;
    }

    /**
     * Returns the reading list for a given sensor, creating it on first access.
     * computeIfAbsent is atomic, so two simultaneous first-access calls are safe.
     */
    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readingTable.computeIfAbsent(
                sensorId,
                key -> Collections.synchronizedList(new ArrayList<>())
        );
    }
}
