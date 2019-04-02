package ru.shemplo.metagennet;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.io.CSVGraphReader;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCDefault;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunMetaGenMCMC {
    
    public static final Random RANDOM = new Random ();
    
    private static final Map <Integer, Double> occurrences = new HashMap <> ();
    
    public static void main (String ... args) throws IOException {
        Locale.setDefault (Locale.ENGLISH);
        
        CSVGraphReader reader = new CSVGraphReader ();
        Graph initial = reader.readGraph ();
        System.out.println (initial);
        
        //GraphHolder mcmc = new GraphHolder (readMatrix (new File ("runtime/graph_good.csv")));
        //List <List <Double>> graphMatrix = readMatrix (GraphGenerator.GEN_FILE);
        //Graph initial = new Graph (graphMatrix, null);
        
        for (int i = 0; i < 1000; i++) {
            Graph copy = initial.makeCopy ();
            copy.setInitial (true);
            
            MCMC singleRun = new MCMCDefault (copy, 1000);
            singleRun.doAllIterations (false);
            
            Graph graph = singleRun.getCurrentGraph ();
            graph.getVertices ().forEach ((id, __) -> 
                occurrences.compute (id, (___, v) -> v == null ? 1 : v + 1)
            );
            //System.out.println (graph);
            
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
        System.out.println ();
        
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
