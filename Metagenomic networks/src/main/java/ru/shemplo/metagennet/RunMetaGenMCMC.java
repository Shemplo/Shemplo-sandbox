package ru.shemplo.metagennet;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.io.AdjMatrixGraphReader;
import ru.shemplo.metagennet.io.GraphReader;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCDefault;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunMetaGenMCMC {
    
    public static final Random RANDOM = new Random ();
    
    private static final Map <Integer, Double> occurrences = new HashMap <> ();
    
    public static void main (String ... args) throws IOException {
        Locale.setDefault (Locale.ENGLISH);
        
        GraphReader reader = new AdjMatrixGraphReader ();
        Graph initial = reader.readGraph ();
        System.out.println (initial);
        
        //GraphHolder mcmc = new GraphHolder (readMatrix (new File ("runtime/graph_good.csv")));
        //List <List <Double>> graphMatrix = readMatrix (GraphGenerator.GEN_FILE);
        //Graph initial = new Graph (graphMatrix, null);
        
        for (int i = 0; i < 10; i++) {
            GraphDescriptor descriptor = initial.getEmptyDescriptor ();
            MCMC singleRun = new MCMCDefault (descriptor, 10000);
            singleRun.doAllIterations (false);
            
            GraphDescriptor graph = singleRun.getCurrentGraph ();
            graph.getVertices ().forEach (vertex -> 
                occurrences.compute (vertex.getId (), (___, v) -> v == null ? 1 : v + 1)
            );
            System.out.println (i + " " + graph);
            
            singleRun.getSnapshots ().forEach (gph -> {
                gph.forEach (vertex -> 
                    occurrences.compute (vertex.getId (), 
                      (___, v) -> v == null ? 1 : v + 1)
                );
            });
            
            if (i % 50 == 0 && i > 0) {
                System.out.print ("=");
            }
        }
        System.out.println ("=");
        
        double sum = occurrences.values ().stream ().mapToDouble (v -> v).sum ();
        occurrences.keySet ().forEach (key -> {
            occurrences.compute (key, (__, v) -> v / sum);
        });
        
        occurrences.entrySet ().stream ()
        . map     (Pair::fromMapEntry)
        . sorted  ((a, b) -> -Double.compare (a.S, b.S))
        . forEach (p -> {
            System.out.println (String.format ("%3d = %.6f", p.F, p.S));
        });
    }
    
    @SuppressWarnings ("unused")
    private static List <List <Double>> readMatrix (File file) throws IOException {
        List <List <Double>> matrix = new ArrayList <> ();
        try (
            InputStream is = new FileInputStream (file);
            Reader r = new InputStreamReader (is);
            BufferedReader br = new BufferedReader (r);
        ) {
            String line = null;
            
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                matrix.add (
                    Arrays.asList (line.split (";")).stream ()
                    . map     (Double::parseDouble)
                    . collect (Collectors.toList ())
                );
            }
        }
        
        return matrix;
    }
    
}
