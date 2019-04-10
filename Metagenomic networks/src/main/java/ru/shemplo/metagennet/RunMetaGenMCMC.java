package ru.shemplo.metagennet;

import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.graph.Vertex;
import ru.shemplo.metagennet.io.CommonWriter;
import ru.shemplo.metagennet.io.GraphReader;
import ru.shemplo.metagennet.io.MelanomaGraphReader;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCFixed;
import ru.shemplo.snowball.stuctures.Pair;

public class RunMetaGenMCMC {
    
    public static final Random RANDOM = new Random ();
    
    private static final int TRIES = 10, ITERATIONS = 1000;
    private static final boolean SIGNALS = true;
    
    private static final BiFunction <GraphDescriptor, Integer, MCMC> SUPPLIER = 
        (graph, iterations) -> new MCMCFixed (graph, iterations);
    
    private static final ExecutorService pool = Executors.newFixedThreadPool (3);
    private static final Map <Vertex, Double> occurrences = new HashMap <> ();
    private static final CommonWriter writer = new CommonWriter ();
    
    public static void main (String ... args) throws IOException, InterruptedException {
        Locale.setDefault (Locale.ENGLISH);
        
        GraphReader reader = new MelanomaGraphReader ();
        Graph initial = reader.readGraph ("paper_");
        System.out.println ("Graph loaded");
        
        /*
        List <Double> aaa = initial.getEdgesList ().stream ()
                          . map     (Edge::getWeight)
                          . collect (Collectors.toList ());
        System.out.println (aaa);
        
        List <Double> bbb = initial.getVertices ().stream ()
                          . map     (Vertex::getWeight)
                          . collect (Collectors.toList ());
        System.out.println (bbb);
        */
       
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
            GraphDescriptor descriptor = initial.getFixedDescriptor (10, SIGNALS);
            System.out.println ("Initial descriptor:");
            System.out.println (descriptor);
            /*
            descriptor.getVertices ().forEach (vertex -> {
                vertex.getEdges ().keySet ().forEach (nei -> {
                    System.out.print (nei.getName () + ", ");
                    //System.out.print (" (" + nei.getId () + ") ");
                });
                System.out.println ();
            });
            //System.out.println (descriptor.getOuterEdges (true));
             */
            
            pool.execute (() -> {
                try {
                    long start = System.currentTimeMillis ();
                    MCMC singleRun = SUPPLIER.apply (descriptor, ITERATIONS);
                    singleRun.doAllIterations (false);
                    
                    long end = System.currentTimeMillis ();
                    System.out.println (String.format ("Run finished in `%s` (time: %.3fs, starts: %d, commits: %d)", 
                                              Thread.currentThread ().getName (), (end - start) / 1000d, 
                                              singleRun.getStarts (), singleRun.getCommits ()));
                    registerRun (singleRun, initial);
                } catch (Exception e) {
                    System.out.println ("Oooops");
                    e.printStackTrace ();
                    registerRun (null, initial);
                }
            });
        }
        
        pool.awaitTermination (1, TimeUnit.DAYS);
    }
    
    private static int registered = 0;
    
    private static synchronized void registerRun (MCMC singleRun, Graph graph) {
        int size = -1;
        if (singleRun != null) {
            GraphDescriptor graphd = singleRun.getCurrentGraph ();
            graph.getVertices ().forEach (vertex -> 
                occurrences.compute (vertex, (___, v) -> v == null ? 1 : v + 1)
            );
            //System.out.println (i + " " + graph);
            
            singleRun.getSnapshots ().forEach (gph -> {
                gph.forEach (vertex -> 
                    occurrences.compute (vertex, (___, v) -> v == null ? 1 : v + 1)
                );
            });
            
            size = singleRun.getCurrentGraph ().getVertices ().size ();
            
            try (
                OutputStream os = new FileOutputStream ("runtime/tmp.log", true);
                PrintWriter pw = new PrintWriter (os);
            ) {
                pw.print   (String.format ("#%04d ", registered + 1));
                pw.println (graphd.toVerticesString ());
            } catch (IOException ioe) {}
        }
        
        System.out.println (String.format ("    %04d registered at ___ %s (verticies: %d)", 
                                           registered + 1, new Date ().toString (), size));
        
        if (++registered == TRIES) {
            printResults (graph); pool.shutdown ();
        }
    }
    
    private static void printResults (Graph graph) {
        writer.saveMap ("runtime/mcmc_frequences.txt", "frequency", occurrences);
        
        double inRun = Math.max (1, ITERATIONS * 0.9 / 100);
        occurrences.keySet ().forEach (key -> {
            occurrences.compute (key, (__, v) -> v / (TRIES * inRun));
        });
        
        writer.saveMap ("runtime/mcmc_results.txt", "probability", occurrences);
        
        /*
        Set <String> orintier = new HashSet <> (Arrays.asList (
            "APEX1", "BCL2", "BCL6", "CD44", "CCND2", "CREBBP", 
            "HCK", "HDAC1", "IL16", "LYN", "IRF4", "MYBL1", 
            "MME", "PTK2", "PIM1", "MAPK10", "PTPN2", "PTPN1", 
            "TGFBR2", "WEE1", "VCL", "BLNK", "BMF", "SH3BP5", 
            "KCNA3"
        ));
        */
        
        Set <String> orintier = new HashSet <> (Arrays.asList (
            "CDKN2A", "MTAP", "MX2", "PARP1", "ARNT", "SETDB1",
            "ATM", "CCND1", "PLA2G6", "RAD23B", "TMEM38B", "FT0",
            "AGR3", "RMDN2", "CASP8", "CDKAL1", "DIP2B", "EBF3",
            "PPP1R15A", "CTR9", "ZNF490", "B2M", "GRAMD3",
            "TOR1AIP1", "MTTP", "ATRIP", "TAF1", "ELAVL1"
        ));
        
        List <Pair <Vertex, Double>> matched = occurrences.entrySet ().stream ()
                                             . map     (Pair::fromMapEntry)
                                             . sorted  ((a, b) -> -Double.compare (a.S, b.S))
                                             . limit   ((int) (orintier.size () * 1.5))
                                             . filter  (pair -> orintier.contains (pair.F.getName ()))
                                             //. forEach (pair -> System.out.println (pair.F.getName ()));
                                             . collect (Collectors.toList ());
        StringJoiner sj = new StringJoiner (", ");
        matched.forEach (pair -> sj.add (String.format ("%s {p: %.4f}", pair.F.getName (), pair.S)));
        System.out.println ("(" + matched.size () + " / " + orintier.size () + ") " + sj.toString ());
        
        GraphDescriptor finalD = graph.getFinalDescriptor (orintier.size (), occurrences);
        writer.saveGradientDOTFile ("runtime/final.dot", finalD, occurrences, Color.GREEN, Color.WHITE);
    }
    
}
