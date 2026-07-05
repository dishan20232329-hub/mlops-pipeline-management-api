package com.mlops;

import com.mlops.config.MLOpsApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class App {
    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        // Wrap our custom JAX-RS application subclass inside a Jersey ResourceConfig
        final ResourceConfig rc = ResourceConfig.forApplication(new MLOpsApplication());

        // Instantiate and start Grizzly HTTP server
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) {
        try {
            final HttpServer server = startServer();
            System.out.println(String.format("Jersey/Grizzly MLOps API server started.\n"
                    + "Discovery Endpoint available at: %s\n"
                    + "WADL definition available at: %sapplication.wadl\n"
                    + "Press Enter (or terminate process) to stop the server...", BASE_URI, BASE_URI));
            
            System.in.read();
            server.shutdownNow();
            System.out.println("Server stopped.");
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Failed to start server", ex);
        }
    }
}
