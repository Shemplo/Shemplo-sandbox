package ru.shemplo.metagennet.graph;

import java.util.*;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCDefault;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor
public class Graph {
    
    @Getter private final boolean oriented;
    
    @Getter private Map <Pair <Vertex, Vertex>, Edge> edges = new HashMap <> ();
    @Getter private Map <Integer, Vertex> vertices = new HashMap <> ();
    @Getter @Setter private GraphModules modules = new GraphModules ();
    
    public GraphDescriptor getEmptyDescriptor (boolean useSignal) {
        GraphDescriptor descriptor = new GraphDescriptor (useSignal, this, new HashSet <> (), new HashSet <> ());
        return descriptor.addEdge (descriptor.selectRandomEdgeFromGraph ()).commit ();
    }
    
    public GraphDescriptor getFixedDescriptor (int verts, boolean useSignal) {
        MCMC mcmc = new MCMCDefault (getEmptyDescriptor (useSignal), 1000000);
        mcmc.makeIteration (true);
        
        while (mcmc.getCurrentGraph ().getVertices ().size () < verts 
                    && !mcmc.finishedWork ()) {
            mcmc.makeIteration (true);
        }
        
        return mcmc.getCurrentGraph ();
    }
    
    public GraphDescriptor getFullDescriptor (boolean useSignal) {
        Set <Vertex> vertices = new HashSet <> (this.vertices.values ());
        Set <Edge> edges = new HashSet <> (this.edges.values ());
        return new GraphDescriptor (useSignal, this, vertices, edges);
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
