package ru.shemplo.metagennet.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.Graph.Vertex;

public class GraphModules {
    
    private final Map <Integer, GraphModule> modules = new HashMap <> ();
    
    public void addModule (GraphModule module) {
        module.vertices.forEach (vertex -> {
            modules.put (vertex.getId (), module);
        });
    }
    
    @RequiredArgsConstructor
    public static class GraphModule {
        
        private final Set <Vertex> vertices;
        
        @Getter @NonNull
        private Double likelihood;
        
    }
    
}
