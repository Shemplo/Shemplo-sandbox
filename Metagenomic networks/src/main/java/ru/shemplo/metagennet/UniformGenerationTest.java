package ru.shemplo.metagennet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Edge;
import ru.shemplo.metagennet.graph.Graph.Vertex;
import ru.shemplo.metagennet.mcmc.MCMCSingleRunHolder;

public class UniformGenerationTest {
    
    private static Graph graph = new Graph (new HashMap <> (), null, new HashSet <> ());
    private static Map <Set <Integer>, Integer> statistics = new HashMap <> ();
    private static Map <Integer, Edge> index2edge = new HashMap <> ();
    
    static {
        List <Vertex> vs = Stream.iterate (0, i -> i + 1).limit (10)
                         . map     (i -> new Vertex (i, 0D))
                         . collect (Collectors.toList ());
        
        int [][] connections = {
            {4, 9}, {5, 9}, {5, 2},
            {9, 2}, {2, 3}, {3, 6},
            {3, 7}, {6, 1}, {1, 8},
            {1, 0}, {7, 0}
        };
        
        int index = 0;
        for (int [] connection : connections) {
            Edge edge = new Edge (vs.get (connection [0]), vs.get (connection [1]), 0D);
            graph = graph.addEdges (false, edge, edge.swap ());
            index2edge.put (index, edge);
            index += 1;
        }
        
        graph.setInitial (true);
        System.out.println (graph);
    }
    
    public static final int MODULE_SIZE = 6, ITERATIONS = 1000 * 10;
    
    public static void main (String ... args) {
        Queue <Set <Integer>> queue = new LinkedList <> ();
        for (int i = 0; i < MODULE_SIZE; i++) {
            if (i == 0) {
                for (int j = 0; j < graph.sizeInEdges () / 2; j++) {
                    Set <Integer> set = new HashSet <> ();
                    set.add (j);
                    
                    queue.add (set);
                }
            } else {
                int queueSize = queue.size ();
                for (int j = 0; j < queueSize; j++) {
                    Set <Integer> tmp = queue.poll ();
                    for (int k = 0; k < graph.sizeInEdges () / 2; k++) {
                        Set <Integer> set = new HashSet <> (tmp);
                        set.add (k);
                        
                        queue.add (set);
                    }
                }
            }
            
            System.out.println (String.format ("Iteration %d finished", i));
        }
        
        int totalLinkedGraphs = (int) queue.stream ()
                              . filter   (set -> set.size () == MODULE_SIZE)
                              . filter   (set -> {
                                  Graph graph = new Graph (new HashMap <> (), null, new HashSet <> ());
                                  List <Edge> toAdd = set.stream ().map (index2edge::get)
                                                    . flatMap (e -> Stream.of (e, e.swap ()))
                                                    . collect (Collectors.toList ());
                                  Edge [] array = toAdd.toArray (new Edge [0]);
                                  try   { graph.addEdges (true, array); } 
                                  catch (Exception e) { return false; }
                                  
                                  return true;
                              })
                              . distinct ().count ();
        double uniformAverage = ITERATIONS * 1.0 / totalLinkedGraphs;
        System.out.println ("Total linked graphs: " + totalLinkedGraphs);
        System.out.println ("Uniform average: " + uniformAverage);
        
        for (int i = 0; i < 1000 * 10; i++) {
            Set <Vertex> module = GraphGenerator.generateModule (graph, 
                                           new Random (), MODULE_SIZE);
            //module = mcmcModuleSelector ();
            
            Set <Integer> set = module.stream ().map (Vertex::getId)
                              . collect (Collectors.toSet ());
            statistics.compute (set, (__, v) -> v == null ? 1 : v + 1);
        }
        
        double deviation = statistics.values ().stream ()
                         . map (v -> v - uniformAverage)
                         . mapToDouble (v -> v * v).sum ();
        System.out.println ("Deviation: " + Math.sqrt (deviation));
    }
    
    public static Set <Vertex> mcmcModuleSelector () {
        MCMCSingleRunHolder runHolder = new MCMCSingleRunHolder (graph, 100);
        runHolder.doAllIterations (true);
        
        return new HashSet <> (runHolder.getCurrentGraph ().getVertices ().values ());
    }
    
}
