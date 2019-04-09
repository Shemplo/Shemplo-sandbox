package ru.shemplo.metagennet;

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
import ru.shemplo.metagennet.io.CSVGraphReader;
import ru.shemplo.metagennet.io.GraphReader;
import ru.shemplo.metagennet.mcmc.MCMC;
import ru.shemplo.metagennet.mcmc.MCMCFixed;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class RunMetaGenMCMC {
    
    public static final Random RANDOM = new Random ();
    
    private static final int TRIES = 20, ITERATIONS = 100000;
    private static final boolean SIGNALS = false;
    
    private static final BiFunction <GraphDescriptor, Integer, MCMC> SUPPLIER = 
        (graph, iterations) -> new MCMCFixed (graph, iterations);
    
    private static final ExecutorService pool = Executors.newFixedThreadPool (3);
    private static final Map <Vertex, Double> occurrences = new HashMap <> ();
    
    public static void main (String ... args) throws IOException, InterruptedException {
        Locale.setDefault (Locale.ENGLISH);
        
        GraphReader reader = new CSVGraphReader (); //new AdjMatrixGraphReader ();
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
            GraphDescriptor full = initial.getFullDescriptor (false);
            pw.println (full.toDot ());
        }
        
        //GraphHolder mcmc = new GraphHolder (readMatrix (new File ("runtime/graph_good.csv")));
        //List <List <Double>> graphMatrix = readMatrix (GraphGenerator.GEN_FILE);
        //Graph initial = new Graph (graphMatrix, null);
        
        for (int i = 0; i < TRIES; i++) {
            GraphDescriptor descriptor = initial.getFixedDescriptor (50, SIGNALS);
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
                    registerRun (singleRun);
                } catch (Exception e) {
                    System.out.println ("Oooops");
                    e.printStackTrace ();
                    registerRun (null);
                }
            });
        }
        
        pool.awaitTermination (1, TimeUnit.DAYS);
    }
    
    private static int registered = 0;
    
    private static synchronized void registerRun (MCMC singleRun) {
        int size = -1;
        if (singleRun != null) {
            GraphDescriptor graph = singleRun.getCurrentGraph ();
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
        }
        
        System.out.println (String.format ("    %04d registered at ___ %s (verticies: %d)", 
                                           registered + 1, new Date ().toString (), size));
        
        if (++registered == TRIES) {
            printResults (); pool.shutdown ();
        }
    }
    
    private static void printResults () {
        printMap ("runtime/mcmc_frequences.txt");
        
        occurrences.keySet ().forEach (key -> {
            occurrences.compute (key, (__, v) -> v / (TRIES * (ITERATIONS * 0.9 / 100)));
        });
        
        printMap ("runtime/mcmc_results.txt");
        
        Set <String> orintier = new HashSet <> (Arrays.asList (
            "APEX1", "BCL2", "BCL6", "CD44", "CCND2", "CREBBP", 
            "HCK", "HDAC1", "IL16", "LYN", "IRF4", "MYBL1", 
            "MME", "PTK2", "PIM1", "MAPK10", "PTPN2", "PTPN1", 
            "TGFBR2", "WEE1", "VCL", "BLNK", "BMF", "SH3BP5", 
            "KCNA3"
        ));
        
        /*
        Set <String> orintier = new HashSet <> (Arrays.asList (
            "CDKN2A", "MTAP", "MX2", "PARP1", "ARNT", "SETDB1",
            "ATM", "CCND1", "PLA2G6", "RAD23B", "TMEM38B", "FT0",
            "AGR3", "RMDN2", "CASP8", "CDKAL1", "DIP2B", "EBF3",
            "PPP1R15A", "CTR9", "ZNF490", "B2M", "GRAMD3",
            "TOR1AIP1", "MTTP", "ATRIP", "TAF1", "ELAVL1"
        ));
        */
        
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
    }
    
    private static void printMap (String filenpath) {
        try (
            PrintWriter pw = new PrintWriter (filenpath);
        ) {
            occurrences.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . sorted  ((a, b) -> -Double.compare (a.S, b.S))
            . forEach (p -> {
                pw.println (String.format ("%9s = %.6f", p.F.getName (), p.S));
            });
        } catch (IOException ioe) {
            ioe.printStackTrace ();
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
