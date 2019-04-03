package ru.shemplo.metagennet.mcmc;

import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.Edge;
import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.graph.Vertex;

@RequiredArgsConstructor
public class MCMCFixed implements MCMC {
    
    public static final double BETA_A_V = 0.2, BETA_B_V = 1;
    public static final double BETA_A_E = 0.1, BETA_B_E = 1;
    
    @Getter protected GraphDescriptor currentGraph;
    protected final GraphDescriptor initialGraph;
    protected List <Edge> initialGraphEdges;
    
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
                snapshots.add (currentGraph.getVertices ());
                //System.out.println (currentGraph);
            }
            //System.out.println (currentGraph.getEdges ());
        }
    }
    
    @Override
    public void makeIteration (boolean idling) {
        if (finishedWork ()) { return; }
        
        if (iteration == 0) {
            currentGraph = initialGraph;
            iteration += 1; return;
        }
        
        double pS = currentGraph.getLikelihood (BETA_A_V, BETA_A_E, true);
        //System.out.println (pS);
        
        //int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        //Edge candidat = initialGraphEdges.get (candidatIndex);
        //System.out.print (candidat + " / " + opposite + " - ");
        Edge candidatIn  = currentGraph.selectRandomEdgeFromSunRays (),
             candidatOut = currentGraph.selectRandomEdgeFromPotato ();
        //System.out.println (currentGraph);
        //System.out.println (candidat);
        
        double qS2Ss = 0, qSs2S = 0;
        qS2Ss = currentGraph.getOuterEdges (true).size ();
        currentGraph.addEdge (candidatIn).removeEdge (candidatOut);
        qS2Ss = currentGraph.getOuterEdges (false).size ();
        
        //System.out.println (currentGraph.getInnerEdges ().size ());
        //System.out.println (currentGraph.getOuterEdges ().size ());
        
        double pSs = currentGraph.getLikelihood (BETA_A_V, BETA_A_E, false);
        //System.out.println (pSs);
        if (idling) { pSs = 1.0; pS = 1.0; } // do not consider likelihood
        double rho = Math.min (1.0, (pSs / pS) * (qSs2S / qS2Ss));
        //System.out.println ("Rho: " + rho);
        
        //System.out.println (rho);
        if (RANDOM.nextDouble () <= rho) {
            currentGraph.isConnected (true, true);
            //System.out.println ("Applied");
            currentGraph.commit ();
        } else {
            currentGraph.rollback ();
        }
        
        //System.out.println (currentGraph);
        iteration += 1;
    }
    
}
