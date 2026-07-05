package com.mlops.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> getDiscoveryInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("version", "v1.0.0");
        info.put("description", "MLOps Pipeline Management API");

        Map<String, String> contact = new HashMap<>();
        contact.put("name", "Hamed Hamzeh");
        contact.put("email", "h.hamzeh@westminster.ac.uk");
        contact.put("role", "Lead Backend Architect / Module Leader");
        info.put("contact", contact);

        Map<String, String> collections = new HashMap<>();
        collections.put("workspaces", "/api/v1/workspaces");
        collections.put("models", "/api/v1/models");
        info.put("collections", collections);

        return info;
    }
}
