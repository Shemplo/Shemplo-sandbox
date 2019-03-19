package ru.shemplo.metagennet.mcmc;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.Graph;

@RequiredArgsConstructor
public class GraphHolder {
    
    @Getter private final List <List <Double>> graph;
    
    public synchronized MCMCSingleRunHolder makeSingleRun (int iterations) {
        return new MCMCSingleRunHolder (new Graph (graph, null), iterations);
    }
    
}
