package ru.shemplo.metagennet.graph;

import java.util.HashMap;
import java.util.Map;

import lombok.*;

@RequiredArgsConstructor
@ToString (exclude = "edges")
public class Vertex {
    
    @Getter private final Map <Vertex, Edge> edges = new HashMap <> ();
    
    @Getter private final int id;
    
    @Setter 
    private String name;
    
    public String getName () {
        if (name != null) {
            return name;
        }
        
        return "V" + id;
    }
    
    @Getter @Setter
    @NonNull private Double weight;
    
    @Override
    public boolean equals (Object obj) {
        if (obj == null) { return false; }
        return hashCode () == obj.hashCode ();
    }
    
    @Override
    public int hashCode () {
        return id;
    }
    
}
