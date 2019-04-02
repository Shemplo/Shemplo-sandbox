package ru.shemplo.metagennet.mcmc;

import static ru.shemplo.metagennet.GraphGenerator.*;
import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ru.shemplo.metagennet.GraphGenerator;
import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Edge;

public class MCMCFixed extends MCMCDefault {
    
    public MCMCFixed (Graph initialGraph, int iterations) {
        super (initialGraph, iterations);
    }
    
    @Override
    public void makeIteration (boolean idling) {
        if (finishedWork ()) { return; }
        
        if (iteration == 0) {
            final int verticies = GraphGenerator.MODULE_SIZE;
            
            initialGraphEdges = new ArrayList <> (initialGraph.getEdges ());
            currentGraph = initialGraph.getInitialSubgraph (verticies);
            iteration += 1; return;
        }
        
        double pS = currentGraph.getLikelihood (BETA_A_V, BETA_A_E);
        
        Set <Edge> toAddSet = currentGraph.getNeighboursE (initialGraphEdges);
        final List <Edge> toAdd = new ArrayList <> (toAddSet);
        int candidatIndex = RANDOM.nextInt (toAdd.size ());
        
        final Edge candidatA = toAdd.get (candidatIndex);
        Edge oppositeA = initialGraph.getOpposite (candidatA);
        
        Set <Edge> toRemoveSet = currentGraph.getEdges ();
        final List <Edge> toRemove = new ArrayList <> (toRemoveSet);
        candidatIndex = RANDOM.nextInt (toRemove.size ());
        
        final Edge candidatB = toRemove.get (candidatIndex);
        Edge oppositeB = initialGraph.getOpposite (candidatB);
        
        Graph suggestedGraph = null;
        double qS2Ss = 0, qSs2S = 0;
        try {
            qS2Ss = currentGraph.getNumberOfOuterEdges (initialGraphEdges);
            suggestedGraph = currentGraph.addEdges (true, candidatA, oppositeA)
                           . removeEdges (true, candidatB, oppositeB);
            qSs2S = suggestedGraph.getNumberOfOuterEdges (initialGraphEdges);
        } catch (IllegalStateException ise) { return; }
        
        double pSs = suggestedGraph.getLikelihood (BETA_A_V, BETA_A_E);
        if (idling) { pSs = 1.0; pS = 1.0; } // do not consider likelihood
        double rho = Math.min (1.0, (pSs / pS) * (qSs2S / qS2Ss));
        
        if (RANDOM.nextDouble () <= rho) {
            currentGraph = suggestedGraph;
        }
        
        iteration += 1;
    }
    
}
