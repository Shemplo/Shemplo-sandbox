package ru.shemplo.metagennet.mcmc;

import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import java.util.ArrayList;
import java.util.HashSet;
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
                snapshots.add (new HashSet <> (currentGraph.getVertices ()));
                //System.out.println (currentGraph.toVerticesString ());
                //System.out.println (currentGraph);
            }
            //System.out.println (currentGraph.getEdges ());
        }
    }
    
    @Getter private int starts = 0;
    
    @Override
    public void makeIteration (boolean idling) {
        if (finishedWork ()) { return; }
        
        starts += 1;
        
        if (iteration == 0) {
            currentGraph = initialGraph;
            iteration += 1; return;
        }
        
        double pS = currentGraph.getLikelihood ();
        //System.out.println (pS);
        
        //System.out.println (currentGraph);
        //int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        //Edge candidat = initialGraphEdges.get (candidatIndex);
        //System.out.print (candidat + " / " + opposite + " - ");
        //System.out.println (currentGraph);
        //System.out.println (candidatIn + " / " + candidatOut);
        
        double qS2Ss = 0, qSs2S = 0;
        qS2Ss = currentGraph.getBorderEdges ();
        
        final Edge candidatBorder = currentGraph.getRandomBorderEdge ();
        final Edge candidatIn     = currentGraph.getRandomInnerEdge ();
        currentGraph.addEdge (candidatBorder).removeEdge (candidatIn);
        //System.out.println ("rm " + candidatIn + " / add " + candidatBorder);
        //System.out.println ("X>> " + currentGraph);
        if (!currentGraph.isConnected ()) {
            //System.out.println ("Not connected");
            currentGraph.rollback (); 
            //iteration += 1;
            
            //System.out.println (">>> " + currentGraph);
            return;
        }
        qSs2S = currentGraph.getBorderEdges ();
        
        //System.out.println (currentGraph.getInnerEdges ().size ());
        //System.out.println (currentGraph.getOuterEdges ().size ());
        
        double pSs = currentGraph.getLikelihood ();
        //System.out.println (pS + " " + pSs + " " + (pSs / pS));
        if (idling) { pSs = 1.0; pS = 1.0; } // do not consider likelihood
        double rho = Math.min (1.0, (pS / pSs) * (qS2Ss / qSs2S));
        //System.out.println ("Rho: " + rho);
        
        //System.out.println (rho);
        if (RANDOM.nextDouble () <= rho) {
            //System.out.println ("Rho: " + rho + ", ps: " + pS + " / " + pSs);
            //System.out.println ("#! " + currentGraph.toVerticesString ());
            //System.out.println ("Applied");
            currentGraph.commit ();
        } else {
            currentGraph.rollback ();
        }
        
        //System.out.println (currentGraph.toVerticesString ());
        //System.out.println (">>> " + currentGraph);
        iteration += 1;
    }
    
}
