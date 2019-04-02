package ru.shemplo.metagennet.graph;

import static ru.shemplo.metagennet.RunMetaGenMCMC.*;

import java.util.*;
import java.util.stream.Collectors;

import lombok.*;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor
public class Graph {
    
    @Getter private final Map <Integer, Vertex> vertices;
    @Getter private final GraphModules graphModules;
    @Getter private final Set <Edge> edges;
    
    @Setter private boolean isInitial = false;
    
    public Graph (List <List <Double>> matrix, GraphModules modules) {
        this.vertices = new HashMap <> ();
        this.edges = new HashSet <> ();
        this.graphModules = modules;
        this.isInitial = true;
        
        for (int i = 0; i < matrix.size (); i++) {
            final double weight = matrix.get (i).get (i);
            vertices.put (i, new Vertex (i, weight));
        }
        
        for (int i = 0; i < matrix.size (); i++) {
            for (int j = 0; j < matrix.size (); j++) {
                if (i == j) { continue; }
                
                double weight = matrix.get (i).get (j);
                if (weight == 0) { continue; }
                
                final Vertex f = vertices.get (i), 
                             s = vertices.get (j);
                
                Edge edge = new Edge (f, s, weight);
                f.edges.put (s, edge);
                edges.add (edge);
            }
        }
    }
    
    @Override
    public String toString () {
        StringJoiner sj = new StringJoiner ("\n");
        
        List <Integer> verts = vertices.values ().stream ()
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
        return vertices.get (edge.S.id).edges.get (edge.F);
    }
    
    public Graph getInitialSubgraph () {
        if (!isInitial) {
            String message = "Initial subgraph can't be make from non-initial graph";
            throw new IllegalStateException (message);
        }
        
        final Graph empty = new Graph (new HashMap <> (), graphModules, new HashSet <> ());
        final Edge edge = new ArrayList <> (edges).get (RANDOM.nextInt (edges.size ()));
        return empty.addEdges (true, edge, getOpposite (edge));
    }
    
    public Graph getInitialSubgraph (int vertices) {
        if (vertices <= 2) { return getInitialSubgraph (); }
        
        if (!isInitial) {
            String message = "Initial subgraph can't be make from non-initial graph";
            throw new IllegalStateException (message);
        }
        
        Graph empty = new Graph (new HashMap <> (), 
                 graphModules, new HashSet <> ());
        
        while (empty.sizeInVertices () < vertices) {
            final int index = RANDOM.nextInt (edges.size ());
            Edge edge = new ArrayList <> (edges).get (index);
            try   { empty = empty.addEdges (true, edge, edge.swap ()); } 
            catch (IllegalStateException ise) {}
        }
        
        return empty;
    }
    
    public Graph makeCopy () {
        Graph graph = new Graph (new HashMap <> (), graphModules, new HashSet <> ());
        
        vertices.forEach ((id, vetex) -> {
            graph.vertices.put (id, new Vertex (id, vetex.weight));
        });
        
        edges.forEach (edge -> {
            Vertex f = graph.vertices.get (edge.F.id),
                   s = graph.vertices.get (edge.S.id);
            Edge e = new Edge (f, s, edge.weight);
            graph.edges.add (e);
            f.edges.put (s, e);
        });
        
        return graph;
    }
    
    public Graph addEdges (boolean checkConnectivity, Edge ... edges) {
        final Graph copy = makeCopy ();
        
        //System.out.println ("\n>>> Current graph:");
        //System.out.println (this.changedE + " / " + this.changedV1 + " / " + this.changedV2 + " / " + isInitial);
        //System.out.println (this);
        //System.out.println ("Edges to add: " + Arrays.toString (edges));
        for (Edge edge : edges) {
            if (this.edges.contains (edge)) { continue; }
            
            Vertex f = new Vertex (edge.F.id, edge.F.weight);
            if (copy.vertices.putIfAbsent (edge.F.id, f) == null) {
                //System.out.println ("Changed vertex F: " + f);
                copy.changedV1 = f;
            }
            f = copy.vertices.get (edge.F.id);
            
            Vertex s = new Vertex (edge.S.id, edge.S.weight);
            if (copy.vertices.putIfAbsent (edge.S.id, s) == null) {
                //System.out.println ("Changed vertex S: " + s);
                copy.changedV2 = s;
            }
            s = copy.vertices.get (edge.S.id);
            
            Edge e = new Edge (f, s, edge.weight);
            copy.changedE = edge;
            f.edges.put (s, e);
            copy.edges.add (e);
        }
        
        int connectivity = isGraphConnected (copy);
        if (checkConnectivity && connectivity != -2) {
            final String message = "Graph is not connected";
            throw new IllegalStateException (message);
        }
        
        //System.out.println (copy.changedE + " / " + copy.changedV1 + " / " + copy.changedV2);
        return copy;
    }
    
    public Graph removeEdges (boolean checkConnectivity, Edge ... edges) {
        final Graph copy = makeCopy ();
        
        //System.out.println ("\n>>> Current graph:");
        //System.out.println (this);
        //System.out.println ("Edges to remove: " + Arrays.toString (edges));
        for (Edge edge : edges) {
            if (!this.edges.contains (edge)) { continue; }
            Vertex f = copy.vertices.get (edge.F.id), 
                   s = copy.vertices.get (edge.S.id);
            copy.edges.remove (edge);
            copy.changedE = edge;
            f.edges.remove (s);
        }
        
        int connectivity = isGraphConnected (copy);
        if (checkConnectivity && connectivity == -1) {
            final String message = "Graph is not connected";
            throw new IllegalStateException (message);
        }
        
        if (checkConnectivity && connectivity != -2) {
            copy.changedV1 = copy.vertices.remove (connectivity);
            //System.out.println ("Changed vertex: " + copy.changedV1);
        }
        
        //System.out.println (copy.changedE + " / " + copy.changedV1 + " / " + copy.changedV2);
        return copy;
    }
    
    private Vertex changedV1, changedV2;
    private Edge   changedE;
    
    public double getLikelihood (double betaAV, double betaAE) {
        /*
        if (isInitial) {
            double pWv = vertices.values ().stream ().mapToDouble (Vertex::getWeight)
                       . reduce (1.0, (a, b) -> a * b);
            double pWe = edges.stream ().mapToDouble (Edge::getWeight)
                       . reduce (1.0, (a ,b) -> a * b);
            return Math.sqrt (pWv * pWe);
        }
        */
        
        //System.out.println (changedE + " " + changedV);
        double pE  = Optional.ofNullable (changedE) .orElse (new Edge (null, null, 1.0)).getWeight ();
        double pV1 = Optional.ofNullable (changedV1).orElse (new Vertex (0, -1.0)).getWeight ();
        double pV2 = Optional.ofNullable (changedV2).orElse (new Vertex (0, -1.0)).getWeight ();
        
        double result = betaAE * Math.pow (pE, betaAE - 1);
        if (pV1 >= 0) {
            result *= betaAV * Math.pow (pV1, betaAV - 1);
        }
        if (pV2 >= 0) {
            result *= betaAV * Math.pow (pV2, betaAV - 1);
        }
        
        return result;
    }
    
    public Set <Vertex> getNeighboursV (Collection <Vertex> vertices) {
        final Set <Vertex> neighbours = new HashSet <> ();
        
        for (Vertex vertex : vertices) {
            if (this.vertices.containsKey (vertex.getId ())) {
                continue; // Vertex already in graph
            }
            
            for (Edge edge : vertex.getEdges ().values ()) {
                if (this.vertices.containsKey (edge.S.getId ())) {
                    neighbours.add (vertex);
                    break;
                }
            }
        }
        
        return neighbours;
    }
    
    public Set <Edge> getNeighboursE (Collection <Edge> edges) {
        final Set <Edge> neighbours = new HashSet <> ();
        
        for (Edge edge : edges) {
            int f = vertices.containsKey (edge.F.id) ? 1 : 0;
            int s = vertices.containsKey (edge.S.id) ? 1 : 0;
            if (f == 1 && s == 0) { neighbours.add (edge); }
        }
        
        return neighbours;
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
            int f = vertices.containsKey (edge.F.id) ? 1 : 0;
            int s = vertices.containsKey (edge.S.id) ? 1 : 0;
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
            int f = vertices.containsKey (edge.F.id) ? 1 : 0;
            int s = vertices.containsKey (edge.S.id) ? 1 : 0;
            innerEdgesNumber += (f + s) / 2;
        }
        
        return innerEdgesNumber;
    }
    
    public int sizeInVertices () {
        return vertices.size ();
    }
    
    public int sizeInEdges () {
        return edges.size ();
    }
    
    @RequiredArgsConstructor
    @ToString (exclude = "edges")
    @EqualsAndHashCode (exclude = {"edges", "weight"})
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
        if (graph.vertices.size () < 2) { return -2; }
        
        
        Queue <Vertex> queue = new LinkedList <> ();
        Set <Vertex> visited = new HashSet <> ();
        
        List <Integer> keys = new ArrayList <> (graph.vertices.keySet ());
        Vertex initial = graph.vertices.get (keys.get (0));
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
        
        int verticies = graph.vertices.size ();
        if (visited.size () == verticies) {
            return -2;
        } else if (visited.size () == verticies - 1) {
            List <Vertex> vertecies = new ArrayList <> (graph.vertices.values ());
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
