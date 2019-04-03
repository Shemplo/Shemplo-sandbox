package ru.shemplo.metagennet.graph;

import java.util.HashMap;
import java.util.Map;

import lombok.*;

@RequiredArgsConstructor
@ToString (exclude = "edges")
@EqualsAndHashCode (exclude = {"edges"})
public class Vertex {
    
    @Getter private final Map <Vertex, Edge> edges = new HashMap <> ();
    
    @Getter private final int id;
    
    @Getter @Setter
    @NonNull private Double weight;
    
    public boolean isConnectedWith (Vertex vertex) {
        return edges.containsKey (vertex);
    }
    
}
