package ru.shemplo.metagennet;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.mcmc.GraphHolder;
import ru.shemplo.metagennet.mcmc.MCMCSingleRunHolder;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunMetaGenMCMC {
    
    public static final Random RANDOM = new Random ();
    
    private static final Map <Integer, Double> occurrences = new HashMap <> ();
    
    public static void main (String ... args) throws IOException {
        Locale.setDefault (Locale.ENGLISH);
        
        //GraphHolder mcmc = new GraphHolder (readMatrix (new File ("runtime/graph_good.csv")));
        GraphHolder mcmc = new GraphHolder (readMatrix (GraphGenerator.GEN_FILE));
        for (int i = 0; i < 1000; i++) {
            MCMCSingleRunHolder singleRun = mcmc.makeSingleRun (10000);
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
        }
        
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
