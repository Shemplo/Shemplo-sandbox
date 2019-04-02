package ru.shemplo.metagennet.mcmc;

import static  ru.shemplo.metagennet.GraphGenerator.*;
import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Edge;
import ru.shemplo.metagennet.graph.Graph.Vertex;

@RequiredArgsConstructor
public class MCMCDefault implements MCMC {
    
    protected final Graph initialGraph;
    
    protected List <Edge> initialGraphEdges;
    @Getter protected Graph currentGraph;
    
    protected final int iterations;
    protected int iteration = 0;
    
    @Getter
    protected final List <Set <Vertex>> snapshots = new ArrayList <> ();
    
    @Override
    public boolean finishedWork () {
        return iteration >= iterations;
    }
    
    @Override
    public void doAllIterations (boolean idling) {
        //System.out.println ("Start");
        while (!finishedWork ()) {
            makeIteration (idling);
            
            int limit = (int) (iterations * 0.9);
            if (iteration >= limit && iteration % 50 == 0) {
                snapshots.add (new HashSet <> (currentGraph.getVertices ().values ()));
            }
            //System.out.println (currentGraph.getEdges ());
        }
    }
    
    @Override
    public void makeIteration (boolean idling) {
        if (finishedWork ()) { return; }
        
        if (iteration == 0) {
            initialGraphEdges = new ArrayList <> (initialGraph.getEdges ());
            currentGraph = initialGraph.getInitialSubgraph ();
            iteration += 1; return;
        }
        
        double pS = currentGraph.getLikelihood (BETA_A_V, BETA_A_E);
        
        int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        Edge candidat = initialGraphEdges.get (candidatIndex);
        Edge opposite = initialGraph.getOpposite (candidat);
        //System.out.print (candidat + " / " + opposite + " - ");
        
        Graph suggestedGraph = null;
        double qS2Ss = 0, qSs2S = 0;
        if (currentGraph.getEdges ().contains (candidat)) {
            //System.out.println ("remove");
            try {
                suggestedGraph = currentGraph.removeEdges (true, candidat, opposite);
            } catch (IllegalStateException ise) { return; }
            
            qSs2S = 1.0 / (currentGraph.getNumberOfInnerEdges (initialGraphEdges));
            qS2Ss = 1.0 / (suggestedGraph.getNumberOfOuterEdges (initialGraphEdges));
        } else {
            //System.out.println ("add");
            try {
                suggestedGraph = currentGraph.addEdges (true, candidat, opposite);
            } catch (IllegalStateException ise) { return; }
            
            qSs2S = 1.0 / (suggestedGraph.getNumberOfInnerEdges (initialGraphEdges));
            qS2Ss = 1.0 / (currentGraph.getNumberOfOuterEdges (initialGraphEdges));
        }
        
        double pSs = suggestedGraph.getLikelihood (BETA_A_V, BETA_A_E);
        if (idling) { pSs = 1.0; pS = 1.0; } // do not consider likelihood
        double rho = Math.min (1.0, (pSs / pS) * (qSs2S / qS2Ss));
        //System.out.println ("Rho: " + rho);
        
        if (RANDOM.nextDouble () <= rho) {
            //System.out.println ("Applied");
            currentGraph = suggestedGraph;
        }
        
        //System.out.println (currentGraph);
        iteration += 1;
    }
    
}
