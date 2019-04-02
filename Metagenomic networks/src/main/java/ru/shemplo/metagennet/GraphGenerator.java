package ru.shemplo.metagennet;

import static java.lang.Math.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Edge;
import ru.shemplo.metagennet.graph.Graph.Vertex;

public class GraphGenerator {
    
    public static final double BETA_A_V = 0.2, BETA_B_V = 1;
    public static final double BETA_A_E = 0.1, BETA_B_E = 1;
    public static final int VERTS = 50, VERTS_DEV = 3; // deviation
    public static final int EDGES = (VERTS - 1) * 2 + 30,
                            EDGES_DEV   = 2;
    public static final int MODULE_SIZE = 7;
    
    public static final File GEN_FILE       = new File ("runtime/graph.csv"),
                             GEN_GRAPH_FILE = new File ("runtime/graph.dot");
    
    private static final Random R = new Random ();
    
    // By default generation will starts from empty graph
    private static Graph graph = new Graph (new HashMap <> (), null, new HashSet <> ());
    
    public static void main (String ... args) throws Exception {
        Locale.setDefault (Locale.ENGLISH);
        
        //
        // Stage 0 - initialization of vertices
        //
        int vertsN = VERTS + VERTS_DEV - R.nextInt (2 * VERTS_DEV + 1);
        Map <Vertex, Set <Integer>> sds = new HashMap <> (); // system of disjoint sets
        for (int i = 0; i < vertsN; i++) {
            Vertex vertex = new Vertex (i, -1D);
            graph.getVertices ().put (i, vertex);
            
            sds.put (vertex, new HashSet <> ());
            sds.get (vertex).add (vertex.getId ());
        }
        
        //
        // Stage 0.5 - initialization of tree
        //
        int sets = sds.size ();
        while (sets != 1) {
            Vertex a = graph.getVertices ().get (R.nextInt (vertsN)),
                   b = graph.getVertices ().get (R.nextInt (vertsN));
            if (!a.equals (b) && !sds.get (a).contains (b.getId ())) {
                final Edge edge = new Edge (a, b, -1D);
                Set <Integer> setA = sds.get (a);
                setA.addAll (sds.get (b));
                
                sds.get (b).forEach (vertex -> {
                    sds.put (graph.getVertices ().get (vertex), setA);
                });
                
                graph = graph.addEdges (false, edge, edge.swap ());
                sets -= 1;
            }
        }
        
        System.out.println (graph);
        
        //
        // Stage 1 - adding additional edges
        //
        int edgesN = EDGES - 2 * R.nextInt (EDGES_DEV * 2 + 1);
        while (graph.sizeInEdges () < edgesN) {
            int a = 0, b = 0, bound = graph.sizeInVertices ();
            while (a == b) {
                a = R.nextInt (bound); b = R.nextInt (bound);
            }
            
            Vertex va = graph.getVertices ().get (a),
                   vb = graph.getVertices ().get (b);
            if (!va.isConnectedWith (vb)) {
                final Edge edge = new Edge (va, vb, -1D);
                graph = graph.addEdges (true, edge, edge.swap ());
            }
        }
        
        graph.setInitial (true);
        System.out.println (graph);
        System.out.println ("Grpah generated");
        
        //
        // Stage 2 - selection signal module
        // 
        Set <Vertex> module = generateModule (graph, R, MODULE_SIZE);
        
        /*
        int initialIndex = R.nextInt (graph.sizeInVertices ());
        module.add (graph.getVertices ().get (initialIndex));
        
        while (module.size () < MODULE_SIZE) {
            int victim = R.nextInt (module.size ());
            
            Vertex vertex = new ArrayList <> (module).get (victim);
            List <Edge> edges = new ArrayList <> (vertex.getEdges ().values ());
            Edge edge = edges.get (R.nextInt (edges.size ()));
            module.add (edge.S);
        }
        */
        
        /*
        MCMCSingleRunHolder runHolder = new MCMCSingleRunHolder (graph, 100);
        runHolder.doAllIterations (true);
        
        module.addAll (runHolder.getCurrentGraph ().getVertices ().values ());
        */
        
        System.out.println (module.stream ().map (Vertex::getId).sorted ().collect (Collectors.toList ()));
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
        graph.getVertices ().forEach ((id, vertex) -> {
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
        int n = graph.sizeInVertices ();
        double [][] matrix = new double [n][n];
        graph.getVertices ().forEach ((id, vertex) -> {
            vertex.getEdges ().forEach ((neighbor, edge) -> {
                matrix [id][neighbor.getId ()] = edge.getWeight ();
            });
            
            matrix [id][id] = vertex.getWeight ();
        });
        
        //
        // Stage 5.5 - symmetries graph matrix
        //
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < i; j++) {
                matrix [i][j] = matrix [j][i];
            }
        }
        
        //
        // Stage 5 - continuation
        //
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
        
        //
        // Stage 6 - visualization of graph
        //
        try (
            OutputStream os = new FileOutputStream (GEN_GRAPH_FILE);
            Writer w = new OutputStreamWriter (os, StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter (w);
        ) {
            pw.println ("graph finite_state_machine {");
            pw.println ("    rankdir=LR;");
            pw.println ("    size=\"24\";");
            pw.println ("    node [shape = doublecircle];");
            pw.println ("    node [color = red];");
            for (Vertex vertex : module) {
                pw.println (String.format ("    V%d;", vertex.getId ()));
            }
            
            pw.println ("    node [shape = circle];");
            pw.println ("    node [color = black];");
            for (Edge edge : graph.getEdges ()) {
                if (edge.F.getId () > edge.S.getId ()) { continue; }
                
                String appendix = "";
                if (module.contains (edge.F) && module.contains (edge.S)) {
                    appendix = "[color = red]";
                }
                pw.println (String.format ("    V%d -- V%d [label = \"%f\"]%s;", 
                            edge.F.getId (), edge.S.getId (),
                            edge.getWeight (), appendix));
            }
            
            pw.println ("}");
        }
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
    
    public static Set <Vertex> generateModule (Graph graph, final Random R, final int MODULE_SIZE) {
        Set <Vertex> module = new HashSet <> ();
        
        int initialIndex = R.nextInt (graph.sizeInVertices ());
        module.add (graph.getVertices ().get (initialIndex));
        
        while (module.size () < MODULE_SIZE) {
            int victim = R.nextInt (module.size ());
            
            Vertex vertex = new ArrayList <> (module).get (victim);
            List <Edge> edges = new ArrayList <> (vertex.getEdges ().values ());
            Edge edge = edges.get (R.nextInt (edges.size ()));
            module.add (edge.S);
        }
        
        return module;
    }
    
}
