package ru.shemplo.metagennet.graph;

import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor
public class Graph {
    
    @Getter private final boolean oriented;
    
    @Getter private Map <Pair <Vertex, Vertex>, Edge> edges = new HashMap <> ();
    @Getter private Map <Integer, Vertex> vertices = new HashMap <> ();
    
    @Override
    public String toString () {
        StringJoiner sj = new StringJoiner ("\n");
        
        List <Integer> verts = vertices.values ().stream ()
                             . map     (Vertex::getId)
                             . collect (Collectors.toList ());
        sj.add (String.format ("Graph #%d", hashCode ()));
        sj.add (String.format (" Verticies : %s", verts.toString ()));
        
        List <String> eds = edges.values ().stream ().map (Edge::toString)
                          . collect (Collectors.toList ());
        sj.add (String.format (" Edges (%2d): %s", eds.size (), eds.toString ()));
        sj.add ("");
        
        return sj.toString ();
    }
    
    public GraphDescriptor getEmptyDescriptor () {
        GraphDescriptor descriptor = new GraphDescriptor (this, new HashSet <> (), new HashSet <> ());
        return descriptor.addEdge (descriptor.selectRandomEdgeFromGraph ()).commit ();
    }
    
    public void addVertex (int id, Double weight) {
        addVertex (new Vertex (id, weight));
    }
    
    public void addVertex (Vertex vertex) {
        int id = vertex.getId ();
        if (vertices.containsKey (id) && !Objects.equals (vertex, vertices.get (id))) {
            throw new IllegalArgumentException ("Vertex " + id + " already exists");
        }
        
        vertices.put (id, vertex);
    }
    
    public void addEdge (int a, int b, Double weight) {
        addEdge (vertices.get (a), vertices.get (b), weight);
    }
    
    public void addEdge (Vertex a, Vertex b, Double weight) {
        addEdge (new Edge (a, b, weight));
    }
    
    public void addEdge (Edge edge) {
        doAddEdge (edge); // If not oriented then two edges should be added
        if (!isOriented ()) { doAddEdge (edge.swap ()); }
    }
    
    private void doAddEdge (Edge edge) {
        Pair <Vertex, Vertex> pair = Pair.mp (edge.F, edge.S);
        if (edges.containsKey (pair) && !Objects.equals (edge, edges.get (pair))) {
            throw new IllegalArgumentException ("Edge " + pair + " already exists");
        }
        
        if (!vertices.containsKey (edge.F.getId ())) {
            addVertex (edge.F);
        }
        
        if (!vertices.containsKey (edge.S.getId ())) {
            addVertex (edge.S);
        }
        
        edge.F.getEdges ().put (edge.S, edge);
        edges.put (pair, edge);
    }
    
}
