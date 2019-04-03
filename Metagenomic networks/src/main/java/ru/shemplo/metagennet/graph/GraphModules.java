package ru.shemplo.metagennet.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class GraphModules {
    
    private final Map <Integer, GraphModule> modules = new HashMap <> ();
    
    public void addModule (GraphModule module) {
        module.vertices.forEach (vertex -> {
            modules.put (vertex.getId (), module);
        });
    }
    
    public Double getLikelihood (Vertex vertex) {
        return modules.get (vertex.getId ()).getLikelihood ();
    }
    
    @RequiredArgsConstructor
    public static class GraphModule {
        
        private final Set <Vertex> vertices;
        
        @Getter @NonNull
        private Double likelihood;
        
    }
    
}
