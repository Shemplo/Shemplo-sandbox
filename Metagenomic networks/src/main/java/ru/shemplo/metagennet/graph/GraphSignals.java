package ru.shemplo.metagennet.graph;

import static java.util.stream.Collectors.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.*;

public class GraphSignals {
    
    public static GraphSignals splitGraph (Graph graph) {
        GraphSignals modules = new GraphSignals ();
        Map <Double, List <Vertex>> mds = graph.getVertices ().stream ()
                                        . filter  (Objects::nonNull)
                                        . collect (groupingBy (Vertex::getWeight));
        AtomicInteger iterator = new AtomicInteger (0);
        mds.forEach ((weight, vertices) -> {
            if (weight <= 0.05) {
                final Set <Vertex> set = new HashSet <> (vertices);
                modules.addSignal (new GraphSignal (iterator.getAndIncrement (),
                                                    set, weight));                
            } else {
                for (Vertex vertex : vertices) {
                    Set <Vertex> set = new HashSet <> ();
                    set.add (vertex);
                    
                    modules.addSignal (new GraphSignal (iterator.getAndIncrement (),
                                                        set, weight));
                }
            }
        });
        
        return modules;
    }
    
    @Getter
    private final Map <Integer, GraphSignal> modules = new HashMap <> ();
    
    public void addSignal (GraphSignal module) {
        module.vertices.forEach (vertex -> {
            modules.put (vertex.getId (), module);
        });
    }
    
    public Double getRatio (Vertex vertex) {
        return modules.get (vertex.getId ()).getLikelihood ();
    }
    
    public GraphSignal getSignal (Vertex vertex) {
        if (vertex == null) { return null; }
        return getSignal (vertex.getId ());
    }
    
    public GraphSignal getSignal (Integer vertexID) {
        if (vertexID == null) { return null; }
        return modules.get (vertexID);
    }
    
    @ToString
    @RequiredArgsConstructor
    @EqualsAndHashCode (exclude = {"likelihood", "vertices"})
    public static class GraphSignal {
        
        @Getter private final int id;
        
        @Getter
        private final Set <Vertex> vertices;
        
        @Getter @NonNull
        private Double likelihood;
        
    }
    
}
