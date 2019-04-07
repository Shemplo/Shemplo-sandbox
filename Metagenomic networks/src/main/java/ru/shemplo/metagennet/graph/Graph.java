package ru.shemplo.metagennet.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCDefault;

@RequiredArgsConstructor
public class Graph {
    
    public static final double BETA_A_V = 0.2, BETA_B_V = 1;
    public static final double BETA_A_E = 0.1, BETA_B_E = 1;
    
    @Getter private List <Edge> edgesList = new ArrayList <> ();
    @Getter private List <Vertex> vertices = new ArrayList <> ();
    @Getter @Setter private GraphModules modules = new GraphModules ();
    
    public GraphDescriptor getEmptyDescriptor (boolean useSignal) {
        GraphDescriptor descriptor = new GraphDescriptor (BETA_A_V, BETA_A_E, useSignal, this);
        Collections.shuffle (edgesList);
        return descriptor.addEdge (edgesList.get (0))
             . commit ();
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
        GraphDescriptor descriptor = new GraphDescriptor (BETA_A_V, BETA_A_E, useSignal, this);
        descriptor.getVertices ().addAll (vertices); 
        descriptor.getEdges ().addAll (edgesList);
        
        return descriptor;
    }
    
    public void addVertex (int id, Double weight) {
        addVertex (new Vertex (id, weight));
    }
    
    public void addVertex (Vertex vertex) {
        int id = vertex.getId ();
        while (id >= vertices.size ()) {
            vertices.add (null);
        }
        
        if (vertices.get (id) != null && !Objects.equals (vertex, vertices.get (id))) {
            throw new IllegalArgumentException ("Vertex " + id + " already exists");
        }
        
        vertices.set (id, vertex);
    }
    
    public void addEdge (int a, int b, Double weight) {
        addEdge (vertices.get (a), vertices.get (b), weight);
    }
    
    public void addEdge (Vertex a, Vertex b, Double weight) {
        addEdge (new Edge (a, b, weight));
    }
    
    public void addEdge (Edge edge) {
        if (vertices.get (edge.F.getId ()) == null) {
            addVertex (edge.F);
        }
        
        if (vertices.get (edge.S.getId ()) == null) {
            addVertex (edge.S);
        }
        
        edge.F.getEdges ().put (edge.S, edge);
        edge.S.getEdges ().put (edge.F, edge);
        edgesList.add (edge);
    }
    
}
