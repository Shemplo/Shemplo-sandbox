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
            
            /*
            if (iteration % 1000 == 0) {
                System.out.println (currentGraph.getVertices ().size () + " " 
                                    + currentGraph.getEdges ().size ());
            }
            */
            
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
        
        //long start = System.currentTimeMillis ();
        if (iteration == 0) {
            currentGraph = initialGraph;
            iteration += 1; return;
        }
        
        double pS = currentGraph.getLikelihood ();
        //System.out.println (pS);
        
        //int candidatIndex = RANDOM.nextInt (initialGraphEdges.size ());
        //Edge candidat = initialGraphEdges.get (candidatIndex);
        //System.out.print (candidat + " / " + opposite + " - ");
        Edge candidat = currentGraph.getRandomGraphEdge (true);
        System.out.println (candidat);
        System.out.println (currentGraph);
        
        double qS2Ss = 0, qSs2S = 0;
        if (currentGraph.getEdges ().contains (candidat)) {
            System.out.println ("remove");
            qS2Ss = 1.0 / Math.max (currentGraph.getInnerEdges (), 1);
            //System.out.println ("R in " + currentGraph.getInnerEdges ());
            if (!currentGraph.removeEdge (candidat).isConnected ()) {
                currentGraph.rollback (); return;
            }
            //System.out.println ("connected");
            
            qSs2S = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
            //System.out.println ("R bord " + currentGraph.getBorderEdges ());
        } else {
            System.out.println ("add");
            qS2Ss = 1.0 / Math.max (currentGraph.getBorderEdges (), 1);
            System.out.println (currentGraph.getBedges ());
            //System.out.println ("A bord " + currentGraph.getBorderEdges ());
            if (!currentGraph.addEdge (candidat).isConnected ()) {
                currentGraph.rollback (); return;
            }
            //System.out.println ("connected");
            
            qSs2S = 1.0 / Math.max (currentGraph.getInnerEdges (), 1);
            //System.out.println ("A in " + currentGraph.getInnerEdges ());
        }
        
        //System.out.println (currentGraph.getInnerEdges ().size ());
        //System.out.println (currentGraph.getOuterEdges ().size ());
        
        double pSs = currentGraph.getLikelihood ();
        System.out.println (pS + " " + pSs + " " + (pSs / pS));
        System.out.println (qSs2S + " " + qS2Ss + " " + (qSs2S / qS2Ss));
        if (idling) { pSs = 1.0; pS = 1.0; } // do not consider likelihood
        double rho = Math.min (1.0, (pSs / pS) * (qSs2S / qS2Ss));
        System.out.println ("Rho: " + rho);
        
        //System.out.println (rho);
        if (RANDOM.nextDouble () <= rho) {
            //System.out.println ("Applied");
            currentGraph.commit ();
        } else {
            currentGraph.rollback ();
        }
        
        System.out.println (currentGraph.toVerticesString ());
        iteration += 1;
        
        //long end = System.currentTimeMillis ();
        //System.out.println (iteration + " " + (end - start) + " ms");
        //System.out.println (currentGraph);
    }
    
}
