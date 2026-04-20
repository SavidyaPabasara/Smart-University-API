package com.uow;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;

/**
 * Bootstraps the embedded Grizzly HTTP server and mounts the JAX-RS application.
 * Press ENTER in the NetBeans output console to stop the server gracefully.
 */
public class ServerLauncher {

    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static void main(String[] args) throws IOException {

        ResourceConfig cfg = ResourceConfig.forApplication(new CampusApplication());

        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(
                URI.create(BASE_URI), cfg
        );

        System.out.println("=====================================================");
        System.out.println("  Smart Campus API is running!");
        System.out.println("  Base URL  : http://localhost:8080/api/v1");
        System.out.println("  Discovery : http://localhost:8080/api/v1");
        System.out.println("  Rooms     : http://localhost:8080/api/v1/rooms");
        System.out.println("  Sensors   : http://localhost:8080/api/v1/sensors");
        System.out.println("  Press ENTER to stop the server...");
        System.out.println("=====================================================");

        System.in.read();           // blocks — press ENTER in the console to stop
        httpServer.shutdownNow();
        System.out.println("Server stopped.");
    }
}
