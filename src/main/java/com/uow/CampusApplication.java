package com.uow;

import com.uow.filter.ApiLoggingFilter;
import com.uow.mapper.CatchAllMapper;
import com.uow.mapper.InvalidReferenceMapper;
import com.uow.mapper.RoomOccupiedMapper;
import com.uow.mapper.SensorOfflineMapper;
import com.uow.resource.ApiRootResource;
import com.uow.resource.RoomController;
import com.uow.resource.SensorController;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application class.
 *
 * @ApplicationPath("/api/v1") declares the versioned base path for the entire API.
 *
 * All components are registered explicitly via getClasses() rather than relying
 * on package scanning — this makes the registration visible and controllable.
 *
 * Per-request lifecycle: JAX-RS creates a brand-new instance of each resource
 * class for every incoming HTTP request. This means resource class fields cannot
 * hold shared state — any data stored there disappears after the request ends.
 * InMemoryStore (singleton) solves this by living outside the resource lifecycle,
 * holding all data in ConcurrentHashMaps that are safe for concurrent access.
 */
@ApplicationPath("/api/v1")
public class CampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> components = new HashSet<>();

        // REST endpoint controllers
        components.add(ApiRootResource.class);
        components.add(RoomController.class);
        components.add(SensorController.class);

        // Exception mappers
        components.add(RoomOccupiedMapper.class);
        components.add(InvalidReferenceMapper.class);
        components.add(SensorOfflineMapper.class);
        components.add(CatchAllMapper.class);

        // Request / response filter
        components.add(ApiLoggingFilter.class);

        return components;
    }
}
