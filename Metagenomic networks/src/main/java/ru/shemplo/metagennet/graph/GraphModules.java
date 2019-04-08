package ru.shemplo.metagennet.graph;

import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.*;

public class GraphModules {
    
    public static GraphModules splitGraph (Graph graph) {
        GraphModules modules = new GraphModules ();
        Map <Double, List <Vertex>> mds = graph.getVertices ().stream ()
                                        . collect (groupingBy (Vertex::getWeight));
        AtomicInteger iterator = new AtomicInteger (0);
        mds.forEach ((weight, vertices) -> {
            if (weight <= 0.05) {
                final Set <Vertex> set = new HashSet <> (vertices);
                modules.addModule (new GraphModule (iterator.getAndIncrement (),
                                                    set, weight));                
            } else {
                for (Vertex vertex : vertices) {
                    Set <Vertex> set = new HashSet <> ();
                    set.add (vertex);
                    
                    modules.addModule (new GraphModule (iterator.getAndIncrement (),
                                                        set, weight));
                }
            }
        });
        
        return modules;
    }
    
    @Getter
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
    
    @ToString
    @RequiredArgsConstructor
    @EqualsAndHashCode (exclude = {"likelihood", "vertices"})
    public static class GraphModule {
        
        private final int id;
        
        @Getter
        private final Set <Vertex> vertices;
        
        @Getter @NonNull
        private Double likelihood;
        
    }
    
}
