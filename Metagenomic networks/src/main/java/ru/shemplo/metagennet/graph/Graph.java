package ru.shemplo.metagennet.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCDefault;

@RequiredArgsConstructor
public class Graph {
    
    private final double alphaV, alphaE;
    
    @Getter private List <Edge> edgesList = new ArrayList <> ();
    @Getter private List <Vertex> vertices = new ArrayList <> ();
    @Getter @Setter private GraphModules modules = new GraphModules ();
    
    public GraphDescriptor getEmptyDescriptor (boolean useSignal) {
        GraphDescriptor descriptor = new GraphDescriptor (alphaV, alphaE, useSignal, this);
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
        GraphDescriptor descriptor = new GraphDescriptor (alphaV, alphaE, useSignal, this);
        descriptor.getVertices ().addAll (vertices); 
        descriptor.getEdges ().addAll (edgesList);
        
        return descriptor;
    }
    
    public Vertex addVertex (int id, Double weight) {
        return addVertex (new Vertex (id, weight));
    }
    
    public Vertex addVertex (Vertex vertex) {
        int id = vertex.getId ();
        while (id >= vertices.size ()) {
            vertices.add (null);
        }
        
        vertices.set (id, vertex);
        return vertex;
    }
    
    public void addEdge (int a, int b, Double weight) {
        addEdge (vertices.get (a), vertices.get (b), weight);
    }
    
    public void addEdge (Vertex a, Vertex b, Double weight) {
        addEdge (new Edge (a, b, weight));
    }
    
    public void addEdge (Edge edge) {
        addVertex (edge.F);
        if (vertices.get (edge.F.getId ()) == null) {
            addVertex (edge.F);
        }
        
        addVertex (edge.S);
        if (vertices.get (edge.S.getId ()) == null) {
            addVertex (edge.S);
        }
        
        edge.F.addEdge (edge);
        edge.S.addEdge (edge);
        edgesList.add (edge);
    }
    
}
