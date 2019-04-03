package ru.shemplo.metagennet.graph;

import static java.util.stream.Collectors.*;

import java.util.*;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ru.shemplo.snowball.stuctures.Pair;

public class GraphModules {
    
    public static GraphModules splitGraph (Graph graph) {
        GraphModules modules = new GraphModules ();
        Map <Double, List <Vertex>> mds = graph.getVertices ().entrySet ().stream ()
                                        . map     (Pair::fromMapEntry).map (Pair::getS)
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
    
    @RequiredArgsConstructor
    public static class GraphModule {
        
        private final Set <Vertex> vertices;
        
        @Getter @NonNull
        private Double likelihood;
        
    }
    
}
