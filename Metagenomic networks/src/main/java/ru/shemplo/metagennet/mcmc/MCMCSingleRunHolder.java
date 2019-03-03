package ru.shemplo.metagennet.mcmc;

import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Edge;

@RequiredArgsConstructor
public class MCMCSingleRunHolder {
    
    private final Graph initialGraph;
    
    private List <Edge> initialGraphEdges;
    private Graph tmpGraph;
    
    private final int iterations;
    private int iteration = 0;
    
    public boolean finishedWork () {
        return iteration >= iterations;
    }
    
    public void doAllIterations () {
        System.out.println ("Start");
        while (!finishedWork ()) {
            makeIteration ();
            
            System.out.println (tmpGraph.getEdges ());
        }
    }
    
    public void makeIteration () {
        if (finishedWork ()) { return; }
        
        if (iteration == 0) {
            initialGraphEdges = new ArrayList <> (initialGraph.getEdges ());
            tmpGraph = initialGraph.getInitialSubgraph ();
            iteration += 1; return;
        }
        
        double pS = tmpGraph.getLikelihood ();
        
        int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        Edge candidat = initialGraphEdges.get (candidatIndex);
        Edge opposite = initialGraph.getOpposite (candidat);
        System.out.print (candidat + " / " + opposite + " - ");
        
        Graph suggestedGraph = null;
        double qS2Ss = 0, qSs2S = 0;
        if (tmpGraph.getEdges ().contains (candidat)) {
            System.out.println ("remove");
            try {
                suggestedGraph = tmpGraph.removeEdges (true, candidat, opposite);
            } catch (IllegalStateException ise) { return; }
            
            qSs2S = 1.0 / (2.0 * tmpGraph.getNumberOfInnerEdges (initialGraphEdges));
            qS2Ss = 1.0 / (2.0 * suggestedGraph.getNumberOfOuterEdges (initialGraphEdges));
        } else {
            System.out.println ("add");
            try {
                suggestedGraph = tmpGraph.addEdges (true, candidat, opposite);
            } catch (IllegalStateException ise) { return; }
            
            qSs2S = 1.0 / (2.0 * suggestedGraph.getNumberOfInnerEdges (initialGraphEdges));
            qS2Ss = 1.0 / (2.0 * tmpGraph.getNumberOfOuterEdges (initialGraphEdges));
        }
        
        double pSs = suggestedGraph.getLikelihood ();
        double rho = Math.min (1.0, (pSs / pS) * (qS2Ss / qSs2S));
        System.out.println ("Rho: " + rho);
        
        if (RANDOM.nextDouble () <= rho) {
            System.out.println ("Applied");
            tmpGraph = suggestedGraph;
        }
        
        iteration += 1;
    }
    
}
