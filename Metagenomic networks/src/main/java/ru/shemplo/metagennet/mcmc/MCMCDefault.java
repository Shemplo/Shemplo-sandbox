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
public class MCMCDefault implements MCMC {
    
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
        
        double pS = currentGraph.getModuleLikelihood (BETA_A_V, BETA_A_E, true);
        //System.out.println (pS);
        
        //int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        //Edge candidat = initialGraphEdges.get (candidatIndex);
        //System.out.print (candidat + " / " + opposite + " - ");
        Edge candidat = currentGraph.selectRandomEdgeFromHedgehog ();
        //System.out.println (currentGraph);
        //System.out.println (candidat);
        
        double qS2Ss = 0, qSs2S = 0;
        if (currentGraph.getEdges ().contains (candidat)) {
            //System.out.println ("remove");
            if (!currentGraph.removeEdge (candidat).isConnected (true, false)) {
                currentGraph.rollback (); return;
            }
            //System.out.println ("connected");
            
            qSs2S = 1.0 / (currentGraph.getInnerEdges (true).size ());
            qS2Ss = 1.0 / (currentGraph.getOuterEdges (false).size ());
        } else {
            //System.out.println ("add");
            if (!currentGraph.addEdge (candidat).isConnected (true, false)) {
                currentGraph.rollback (); return;
            }
            //System.out.println ("connected");
            
            qSs2S = 1.0 / (currentGraph.getInnerEdges (false).size ());
            qS2Ss = 1.0 / (currentGraph.getOuterEdges (true).size ());
        }
        
        //System.out.println (currentGraph.getInnerEdges ().size ());
        //System.out.println (currentGraph.getOuterEdges ().size ());
        
        double pSs = currentGraph.getModuleLikelihood (BETA_A_V, BETA_A_E, false);
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
