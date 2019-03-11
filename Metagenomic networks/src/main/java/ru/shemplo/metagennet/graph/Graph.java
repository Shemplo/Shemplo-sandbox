package ru.shemplo.metagennet.graph;

import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import java.util.*;
import java.util.stream.Collectors;

import lombok.*;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor
public class Graph {
    
    @Getter private final Map <Integer, Vertex> verticies;
    @Getter private final Set <Edge> edges;
    
    private boolean isInitial = false;
    
    public Graph (List <List <Double>> matrix) {
        this.verticies = new HashMap <> ();
        this.edges = new HashSet <> ();
        this.isInitial = true;
        
        for (int i = 0; i < matrix.size (); i++) {
            final double weight = matrix.get (i).get (i);
            verticies.put (i, new Vertex (i, weight));
        }
        
        for (int i = 0; i < matrix.size (); i++) {
            for (int j = 0; j < matrix.size (); j++) {
                if (i == j) { continue; }
                
                double weight = matrix.get (i).get (j);
                if (weight == 0) { continue; }
                
                final Vertex f = verticies.get (i), 
                             s = verticies.get (j);
                
                Edge edge = new Edge (f, s, weight);
                f.edges.put (s, edge);
                edges.add (edge);
            }
        }
    }
    
    @Override
    public String toString () {
        StringJoiner sj = new StringJoiner ("\n");
        
        List <Integer> verts = verticies.values ().stream ()
                             . map     (Vertex::getId)
                             . collect (Collectors.toList ());
        sj.add (String.format ("Graph #%d", hashCode ()));
        sj.add (String.format (" Verticies : %s", verts.toString ()));
        
        List <String> eds = edges.stream ().map (Edge::toString)
                          . collect (Collectors.toList ());
        sj.add (String.format (" Edges (%2d): %s", eds.size (), eds.toString ()));
        sj.add ("");
        
        return sj.toString ();
    }
    
    public Edge getOpposite (Edge edge) {
        return verticies.get (edge.S.id).edges.get (edge.F);
    }
    
    public Graph getInitialSubgraph () {
        if (!isInitial) {
            String message = "Initial subgraph can't be make from non-initial graph";
            throw new IllegalStateException (message);
        }
        
        Edge edge = new ArrayList <> (edges).get (RANDOM.nextInt (edges.size ()));
        final Graph empty = new Graph (new HashMap <> (), new HashSet <> ());
        return empty.addEdges (true, edge, getOpposite (edge));
    }
    
    public Graph makeCopy () {
        Graph graph = new Graph (new HashMap <> (), new HashSet <> ());
        
        verticies.forEach ((id, vetex) -> {
            graph.verticies.put (id, new Vertex (id, vetex.weight));
        });
        
        edges.forEach (edge -> {
            Vertex f = graph.verticies.get (edge.F.id),
                   s = graph.verticies.get (edge.S.id);
            Edge e = new Edge (f, s, edge.weight);
            graph.edges.add (e);
            f.edges.put (s, e);
        });
        
        return graph;
    }
    
    public Graph addEdges (boolean checkConnectivity, Edge ... edges) {
        final Graph copy = makeCopy ();
        
        for (Edge edge : edges) {
            if (this.edges.contains (edge)) { continue; }
            
            Vertex f = new Vertex (edge.F.id, edge.F.weight);
            copy.verticies.putIfAbsent (edge.F.id, f);
            f = copy.verticies.get (edge.F.id);
            
            Vertex s = new Vertex (edge.S.id, edge.S.weight);
            copy.verticies.putIfAbsent (edge.S.id, s);
            s = copy.verticies.get (edge.S.id);
            
            Edge e = new Edge (f, s, edge.weight);
            f.edges.put (s, e);
            copy.edges.add (e);
        }
        
        int connectivity = isGraphConnected (copy);
        if (checkConnectivity && connectivity != -2) {
            final String message = "Graph is not connected";
            throw new IllegalStateException (message);
        }
        
        return copy;
    }
    
    public Graph removeEdges (boolean checkConnectivity, Edge ... edges) {
        final Graph copy = makeCopy ();
        
        for (Edge edge : edges) {
            if (!this.edges.contains (edge)) { continue; }
            Vertex f = copy.verticies.get (edge.F.id), 
                   s = copy.verticies.get (edge.S.id);
            copy.edges.remove (edge);
            f.edges.remove (s);
        }
        
        int connectivity = isGraphConnected (copy);
        if (checkConnectivity && connectivity == -1) {
            final String message = "Graph is not connected";
            throw new IllegalStateException (message);
        }
        
        if (connectivity != -2) {
            copy.verticies.remove (connectivity);
        }
        
        return copy;
    }
    
    public double getLikelihood () {
        double pWv = verticies.values ().stream ().mapToDouble (Vertex::getWeight)
                   . reduce (1.0, (a, b) -> a * b);
        double pWe = edges.stream ().mapToDouble (Edge::getWeight)
                   . reduce (1.0, (a ,b) -> a * b);
        return Math.sqrt (pWv * pWe);
    }
    
    /**
     * Only one end of the edge belongs to the graph
     * 
     * @param edges
     * 
     * @return
     * 
     */
    public int getNumberOfOuterEdges (Collection <Edge> edges) {
        int outerEdgesNumber = 0;
        for (Edge edge : edges) {
            int f = verticies.containsKey (edge.F.id) ? 1 : 0;
            int s = verticies.containsKey (edge.S.id) ? 1 : 0;
            outerEdgesNumber += (f + s) & 2;
        }
        
        return outerEdgesNumber;
    }
    
    /**
     * Two ends of edge belong to the graph
     * 
     * @param edges
     * 
     * @return
     * 
     */
    public int getNumberOfInnerEdges (Collection <Edge> edges) {
        int innerEdgesNumber = 0;
        for (Edge edge : edges) {
            int f = verticies.containsKey (edge.F.id) ? 1 : 0;
            int s = verticies.containsKey (edge.S.id) ? 1 : 0;
            innerEdgesNumber += (f + s) / 2;
        }
        
        return innerEdgesNumber;
    }
    
    public int sizeInVerticies () {
        return verticies.size ();
    }
    
    public int sizeInEdges () {
        return edges.size ();
    }
    
    @RequiredArgsConstructor
    @ToString (exclude = "edges")
    @EqualsAndHashCode (exclude = "edges")
    public static class Vertex {
        
        @Getter private final Map <Vertex, Edge> edges = new HashMap <> ();
        
        @Getter private final int id;
        
        @Getter @Setter
        @NonNull private Double weight;
        
        public boolean isConnectedWith (Vertex vertex) {
            return edges.containsKey (vertex);
        }
        
    }
    
    @EqualsAndHashCode (exclude = {"weight"}, callSuper = true)
    public static class Edge extends Pair <Vertex, Vertex> {

        private static final long serialVersionUID = -33766014367150868L;
        
        @Getter @Setter
        @NonNull private Double weight;
        
        public Edge (Vertex F, Vertex S, double weight) { 
            super (F, S); this.weight = weight;
        }
        
        @Override
        public String toString () {
            return String.format ("%d -> %d", F.id, S.id);
        }
        
        @Override
        public Edge swap () { return new Edge (S, F, weight); }
        
    }
    
    /**
     * 
     * @param graph
     * 
     * @return -1 if graph is not connected, -2 if graph is connected,
     *         i <i>(i >= 0)</i> if need to remove i-th vertex to make
     *         graph connected
     * 
     */
    public static int isGraphConnected (Graph graph) {
        if (graph.verticies.size () < 2) { return -2; }
        
        
        Queue <Vertex> queue = new LinkedList <> ();
        Set <Vertex> visited = new HashSet <> ();
        
        List <Integer> keys = new ArrayList <> (graph.verticies.keySet ());
        Vertex initial = graph.verticies.get (keys.get (0));
        visited.add (initial);
        queue.add (initial);
        
        while (!queue.isEmpty ()) {
            Vertex vertex = queue.poll ();
            vertex.edges.forEach ((vert, edge) -> {
                if (!visited.contains (edge.S)) {
                    visited.add (edge.S);
                    queue.add (edge.S);
                }
            });
        }
        
        int verticies = graph.verticies.size ();
        if (visited.size () == verticies) {
            return -2;
        } else if (visited.size () == verticies - 1) {
            List <Vertex> vertecies = new ArrayList <> (graph.verticies.values ());
            Collections.shuffle (vertecies, RANDOM);
            
            for (Vertex vertex : vertecies) {
                if (!visited.contains (vertex)) {
                    return vertex.id;
                }
            }
        }
        
        return -1;
    }
    
}
