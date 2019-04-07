package ru.shemplo.metagennet;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.io.GraphReader;
import ru.shemplo.metagennet.io.MelanomaGraphReader;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCFixed;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunMetaGenMCMC {
    
    public static final Random RANDOM = new Random ();
    
    private static final int TRIES = 1, ITERATIONS = 100000;
    
    private static final ExecutorService pool = Executors.newFixedThreadPool (4);
    private static final Map <Integer, Double> occurrences = new HashMap <> ();
    
    public static void main (String ... args) throws IOException, InterruptedException {
        Locale.setDefault (Locale.ENGLISH);
        
        GraphReader reader = new MelanomaGraphReader ();
        Graph initial = reader.readGraph ();
        System.out.println ("Graph loaded");
       
        try (
            PrintWriter pw = new PrintWriter (new File ("runtime/temp.dot"));
        ) {
            @SuppressWarnings ("unused")
            GraphDescriptor full = initial.getFullDescriptor (false);
            //pw.println (full.toDot ());
        }
        
        //GraphHolder mcmc = new GraphHolder (readMatrix (new File ("runtime/graph_good.csv")));
        //List <List <Double>> graphMatrix = readMatrix (GraphGenerator.GEN_FILE);
        //Graph initial = new Graph (graphMatrix, null);
        
        for (int i = 0; i < TRIES; i++) {
            GraphDescriptor descriptor = initial.getFixedDescriptor (5, false);
            //System.out.println ("Initial descriptor:");
            //System.out.println (descriptor);
            //System.out.println (descriptor.getOuterEdges (true));
            
            pool.execute (() -> {
                long start = System.currentTimeMillis ();
                MCMC singleRun = new MCMCFixed (descriptor, ITERATIONS);
                singleRun.doAllIterations (false);
                
                long end = System.currentTimeMillis ();
                System.out.println (String.format ("Run finished in `%s` (time: %.3fs, starts: %d)", 
                            Thread.currentThread ().getName (), (end - start) / 1000d, singleRun.getStarts ()));
                registerRun (singleRun);
            });
        }
        
        pool.awaitTermination (1, TimeUnit.DAYS);
    }
    
    private static int registered = 0;
    
    private static synchronized void registerRun (MCMC singleRun) {
        GraphDescriptor graph = singleRun.getCurrentGraph ();
        graph.getVertices ().forEach (vertex -> 
            occurrences.compute (vertex.getId (), (___, v) -> v == null ? 1 : v + 1)
        );
        //System.out.println (i + " " + graph);
        
        singleRun.getSnapshots ().forEach (gph -> {
            gph.forEach (vertex -> 
                occurrences.compute (vertex.getId (), 
                   (___, v) -> v == null ? 1 : v + 1)
            );
        });
        
        if (++registered == TRIES) {
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
            
            pool.shutdown ();
        }
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
