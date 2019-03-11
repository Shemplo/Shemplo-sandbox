package ru.shemplo.metagennet;

import static java.lang.Math.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Edge;
import ru.shemplo.metagennet.graph.Graph.Vertex;

public class GraphGenerator {
    
    public static final double BETA_A_V = 3, BETA_B_V = 2;
    public static final double BETA_A_E = 2, BETA_B_E = 1;
    public static final int VERTS = 40, VERTS_DEV = 3; // deviation
    public static final int EDGES = (VERTS - 1) * 2 + 30,
                            EDGES_DEV = 2;
    public static final int MODULE_SIZE = 4;
    
    public static final File GEN_FILE = new File ("runtime/graph.csv");
    
    private static final Random R = new Random ();
    
    // By default generation will starts from empty graph
    private static Graph graph = new Graph (new HashMap <> (), new HashSet <> ());
    
    public static void main (String ... args) throws Exception {
        graph.getVerticies ().put (0, new Vertex (0, -1D)); // graph of 1 vertex
        
        //
        // Stage 1 - graph dummy
        //
        boolean enoughVerticies = false, enoughEdges = false;
        while (!enoughVerticies || !enoughEdges) {
            if (!enoughVerticies && addVertex ()) {
                final int bound = graph.sizeInVerticies ();
                Vertex vertex = new Vertex (bound, -1D);
                
                final int neigborIndex = R.nextInt (bound);
                Vertex neighbor = graph.getVerticies ()
                                . get (neigborIndex);
                
                Edge edge = new Edge (vertex, neighbor, -1D);
                graph = graph.addEdges (true, edge, edge.swap ());
            } else if (!enoughEdges && graph.sizeInVerticies () > VERTS / 5) {
                int a = 0, b = 0, bound = graph.sizeInVerticies ();
                while (a == b) {
                    a = R.nextInt (bound); b = R.nextInt (bound);
                }
                
                Vertex va = graph.getVerticies ().get (a),
                       vb = graph.getVerticies ().get (b);
                if (!va.isConnectedWith (vb)) {
                    Edge edge = new Edge (va, vb, -1D);
                    graph = graph.addEdges (true, edge, edge.swap ());
                }
            }
            
            enoughVerticies = graph.sizeInVerticies () >= VERTS + VERTS_DEV - R.nextInt (VERTS_DEV * 2 + 1);
            enoughEdges     = graph.sizeInEdges () >= EDGES - 2 * R.nextInt (EDGES_DEV * 2 + 1);
        }
        
        System.out.println (graph);
        System.out.println ("Grpah generated");
        
        //
        // Stage 2 - selection signal module
        // 
        Set <Vertex> module = new HashSet <> ();
        
        int initialIndex = R.nextInt (graph.sizeInVerticies ());
        module.add (graph.getVerticies ().get (initialIndex));
        
        while (module.size () < MODULE_SIZE) {
            int victim = R.nextInt (module.size ());
            
            Vertex vertex = new ArrayList <> (module).get (victim);
            List <Edge> edges = new ArrayList <> (vertex.getEdges ().values ());
            Edge edge = edges.get (R.nextInt (edges.size ()));
            module.add (edge.S);
        }
        
        System.out.println (module);
        System.out.println ("Signal module selected");
        
        //
        // Stage 3 - beta distribution in module
        //
        module.forEach (vertex -> {
            vertex.getEdges ().forEach ((neighbor, edge) -> {
                // If edge in module -> beta distribution
                // Else              -> beta distribution with chance 0.5
                if (module.contains (neighbor) || R.nextBoolean ()) {
                    edge.setWeight (nextBetaDouble (BETA_A_E, BETA_B_E));
                }
            });
            
            vertex.setWeight (nextBetaDouble (BETA_A_V, BETA_B_V));
        });
        
        System.out.println ("Beta distribution applied");
        
        //
        // Stage 4 - uniform distribution on rest graph
        //
        graph.getVerticies ().forEach ((id, vertex) -> {
            if (vertex.getWeight () != -1D) { return; }
            vertex.setWeight (R.nextDouble ());
        });
        
        graph.getEdges ().forEach (edge -> {
            if (edge.getWeight () != -1D) { return; }
            edge.setWeight (R.nextDouble ());
        });
        
        System.out.println ("Uniform distribution applied");
        
        //
        // Stage 5 - publishing graph
        //
        int n = graph.sizeInVerticies ();
        double [][] matrix = new double [n][n];
        graph.getVerticies ().forEach ((id, vertex) -> {
            vertex.getEdges ().forEach ((neighbor, edge) -> {
                matrix [id][neighbor.getId ()] = edge.getWeight ();
            });
            
            matrix [id][id] = vertex.getWeight ();
        });
        
        GEN_FILE.getParentFile ().mkdirs ();
        try (
            OutputStream os = new FileOutputStream (GEN_FILE);
            Writer w = new OutputStreamWriter (os, StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter (w);
        ) {
            for (int i = 0; i < matrix.length; i++) {
                StringJoiner sj = new StringJoiner (";");
                
                for (int j = 0; j < matrix [i].length; j++) {
                    sj.add (String.format (Locale.ENGLISH, "%.7f", matrix [i][j]));
                }
                
                pw.println (sj.toString ());
            }
        }
        
        System.out.println ("Graph saved in file");
    }
    
    public static boolean addVertex () {
        return R.nextBoolean ();
    }
    
    public static double nextBetaDouble (double a, double b) {
        double s1 = 1, s2 = 1;
        while (s1 + s2 > 1 || s1 + s2 == 0.0) {
            s1 = nthroot (a, R.nextDouble ());
            s2 = nthroot (b, R.nextDouble ());
        }
        
        return s1 / (s1 + s2);
    }
    
    public static double nthroot (double root, double value) {
        return signum (value) * pow (abs (value), 1.0 / root);
    }
    
}
