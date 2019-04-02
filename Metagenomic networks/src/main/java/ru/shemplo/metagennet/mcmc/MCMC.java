package ru.shemplo.metagennet.mcmc;

import java.util.List;
import java.util.Set;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Vertex;

public interface MCMC {
    
    public void doAllIterations (boolean idling);
    
    public boolean finishedWork ();
    
    public Graph getCurrentGraph ();
    
    public List <Set <Vertex>> getSnapshots ();
    
    public void makeIteration (boolean idling);
    
}
