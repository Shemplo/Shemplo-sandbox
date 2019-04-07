package ru.shemplo.metagennet.graph;

import static java.util.stream.Collectors.*;

import java.util.*;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class GraphModules {
    
    public static GraphModules splitGraph (Graph graph) {
        GraphModules modules = new GraphModules ();
        Map <Double, List <Vertex>> mds = graph.getVertices ().stream ()
                                        . collect (groupingBy (Vertex::getWeight));
        mds.forEach ((weight, vertices) -> {
            final Set <Vertex> set = new HashSet <> (vertices);
            modules.addModule (new GraphModule (set, weight));
        });
        
        return modules;
    }
    
    private final Map <Integer, GraphModule> modules = new HashMap <> ();
    
    public void addModule (GraphModule module) {
        module.vertices.forEach (vertex -> {
            modules.put (vertex.getId (), module);
        });
    }
    
    public Double getLikelihood (Vertex vertex) {
        return modules.get (vertex.getId ()).getLikelihood ();
    }
    
    public GraphModule getModule (Vertex vertex) {
        if (vertex == null) { return null; }
        return getModule (vertex.getId ());
    }
    
    public GraphModule getModule (Integer vertexID) {
        if (vertexID == null) { return null; }
        return modules.get (vertexID);
    }
    
    @RequiredArgsConstructor
    @EqualsAndHashCode (exclude = {"likelihood"})
    public static class GraphModule {
        
        @Getter
        private final Set <Vertex> vertices;
        
        @Getter @NonNull
        private Double likelihood;
        
    }
    
}
