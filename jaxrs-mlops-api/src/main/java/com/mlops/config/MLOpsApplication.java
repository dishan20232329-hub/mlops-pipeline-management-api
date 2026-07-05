package com.mlops.config;

import com.mlops.resource.DiscoveryResource;
import com.mlops.resource.WorkspaceResource;
import com.mlops.resource.ModelResource;
import com.mlops.filter.LoggingFilter;
import com.mlops.exception.WorkspaceNotEmptyExceptionMapper;
import com.mlops.exception.LinkedWorkspaceNotFoundExceptionMapper;
import com.mlops.exception.ModelDeprecatedExceptionMapper;
import com.mlops.exception.GlobalExceptionMapper;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/api/v1")
public class MLOpsApplication extends Application {
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        
        // Register REST API Resources
        classes.add(DiscoveryResource.class);
        classes.add(WorkspaceResource.class);
        classes.add(ModelResource.class);
        
        // Register Observability Filters
        classes.add(LoggingFilter.class);
        
        // Register Custom Exception Mappers
        classes.add(WorkspaceNotEmptyExceptionMapper.class);
        classes.add(LinkedWorkspaceNotFoundExceptionMapper.class);
        classes.add(ModelDeprecatedExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);
        
        // Register Jackson JSON Feature
        classes.add(org.glassfish.jersey.jackson.JacksonFeature.class);
        
        return classes;
    }
}
