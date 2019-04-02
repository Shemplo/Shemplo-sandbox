package ru.shemplo.metagennet.mcmc;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.Graph;

@RequiredArgsConstructor
public class GraphHolder {
    
    @Getter private final List <List <Double>> graph;
    
    public synchronized MCMCDefault makeSingleRun (int iterations) {
        return new MCMCDefault (new Graph (graph, null), iterations);
    }
    
}
