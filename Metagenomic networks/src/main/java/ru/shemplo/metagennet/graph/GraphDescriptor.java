package ru.shemplo.metagennet.graph;

import java.util.*;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.snowball.stuctures.Pair;

@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
public class GraphDescriptor implements Cloneable {
    
    private final boolean signal;
    private final Graph graph;
    
    @Getter private final Set <Vertex> vertices;
    @Getter private final Set <Edge> edges;
    
    private Vertex pmVA, pmVB; // previous modified Vertex (A | B)
    private Edge pmEA;         // previous modified Edge
    
    private Vertex lmVA, lmVB; // last modified Vertex (A | B)
    private Edge lmEA;         // last modified Edge
    
    private final Set <Vertex> vertexToChange = new HashSet <> ();
    private final Set <Edge> edgeToChange = new HashSet <> ();
    
    @Override
    public String toString () {
        StringJoiner sj = new StringJoiner ("\n");
        
        List <Integer> verts = vertices.stream ()
                             . map     (Vertex::getId)
                             . sorted  ()
                             . collect (Collectors.toList ());
        sj.add (String.format ("Graph #%d", hashCode ()));
        sj.add (String.format (" Verticies : %s", verts.toString ()));
        
        List <String> eds = edges.stream ().map (Edge::toString)
                          . collect (Collectors.toList ());
        sj.add (String.format (" Edges (%2d): %s", eds.size (), eds.toString ()));
        sj.add ("");
        
        return sj.toString ();
    }
    
    public String toDot () {
        StringJoiner sj = new StringJoiner ("\n");
        sj.add ("graph finite_state_machine {");
        sj.add ("    rankdir=LR;");
        sj.add ("    size=\"64\";");
        sj.add ("    node [shape = doublecircle];");
        sj.add ("    node [color = red];");
        for (Vertex vertex : vertices) {
            sj.add (String.format ("    V%d;", vertex.getId ()));
        }
        
        sj.add ("    node [shape = circle];");
        sj.add ("    node [color = black];");
        
        for (Edge edge : graph.getEdges ().values ()) {
            if (edge.F.getId () > edge.S.getId ()) { continue; }
            
            String appendix = "";
            if (vertices.contains (edge.F) && vertices.contains (edge.S)) {
                appendix = "[color = red]";
            }
            sj.add (String.format ("    V%d -- V%d [label = \"%f\"]%s;", 
                    edge.F.getId (), edge.S.getId (),
                    edge.getWeight (), appendix));
        }
        
        sj.add ("}");
        
        return sj.toString ();
    }
    
    private static final Edge STUB_EDGE = new Edge (null, null, 1.0);
    private static final Vertex STUB_VERTEX = new Vertex (0, 1.0);
    private static final Random R = new Random ();
    
    public double getLikelihood (double betaAV, double betaAE, boolean stable) {
        double pE  = Optional.ofNullable (stable ? pmEA : lmEA).orElse (STUB_EDGE)  .getWeight ();
        double pV1 = Optional.ofNullable (stable ? pmVA : lmVA).orElse (STUB_VERTEX).getWeight ();
        double pV2 = Optional.ofNullable (stable ? pmVB : lmVB).orElse (STUB_VERTEX).getWeight ();
        
        //System.out.println (pmEA + " " + lmEA.getWeight ());
        //System.out.println (pE + " " + pV1 + " " + pV2);
        double result = betaAE * Math.pow (pE, betaAE);
        if (pV1 >= 0) {
            result *= betaAV * Math.pow (pV1, betaAV);
        }
        if (pV2 >= 0) {
            result *= betaAV * Math.pow (pV2, betaAV);
        }
        
        return result;
    }
    
    public double getModuleLikelihood (double betaAV, double betaAE, boolean stable) {
        double pE  = Optional.ofNullable (stable ? pmEA : lmEA).orElse (STUB_EDGE)  .getWeight ();
        double pV1 = Optional.ofNullable (stable ? pmVA : lmVA).orElse (STUB_VERTEX).getWeight ();
        double pV2 = Optional.ofNullable (stable ? pmVB : lmVB).orElse (STUB_VERTEX).getWeight ();
        
        //System.out.println (pmEA + " " + lmEA.getWeight ());
        //System.out.println (pE + " " + pV1 + " " + pV2);
        double result = betaAE * Math.pow (pE, betaAE);
        if (pV1 >= 0) {
            result *= betaAV * Math.pow (pV1, betaAV);
        }
        if (pV2 >= 0) {
            result *= betaAV * Math.pow (pV2, betaAV);
        }
        
        return result;
    }
    
    public GraphDescriptor commit () {
        vertexToChange.forEach (vertex -> {
            if (vertices.contains (vertex)) {
                vertices.remove (vertex);
            } else {
                vertices.add (vertex);
            }
        });
        
        edgeToChange.forEach (edge -> {
            if (edges.contains (edge)) {
                edges.remove (edge);
            } else {
                edges.add (edge);
            }
        });
        
        vertexToChange.clear ();
        edgeToChange.clear ();
        
        pmVA = lmVA; pmVB = lmVB;
        pmEA = lmEA;
        
        return this;
    }
    
    public GraphDescriptor rollback () {
        vertexToChange.clear ();
        edgeToChange.clear ();
        
        return this;
    }
    
    public GraphDescriptor addVertex (Vertex vertex) {
        return addVertex (vertex.getId ());
    }
    
    public GraphDescriptor addVertex (int id) {
        final Vertex va = graph.getVertices ().get (id);
        if (!vertexToChange.contains (va) && !vertices.contains (va)) { 
            vertexToChange.add (va); lmVA = va;
        }
        
        return this;
    }
    
    public GraphDescriptor addEdge (Edge edge) {
        return addEdge (edge.F, edge.S);
    }
    
    public GraphDescriptor addEdge (Vertex a, Vertex b) {
        return addEdge (a.getId (), b.getId ());
    }
    
    public GraphDescriptor addEdge (int a, int b) {
        addVertex (a); lmVB = lmVA;
        lmVA = null; addVertex (b);
        
        final Vertex va = graph.getVertices ().get (a),
                     vb = graph.getVertices ().get (b);
        final Pair <Vertex, Vertex> pair = Pair.mp (va, vb);
        
        doAddEdge (pair); // If not oriented then two edges should be added
        if (!graph.isOriented ()) { doAddEdge (pair.swap ()); }
        return this;
    }
    
    private void doAddEdge (Pair <Vertex, Vertex> pair) {
        edgeToChange.add (lmEA = graph.getEdges ().get (pair));
    }
    
    public GraphDescriptor removeEdge (Edge edge) {
        return removeEdge (edge.F, edge.S);
    }
    
    public GraphDescriptor removeEdge (Vertex a, Vertex b) {
        return removeEdge (a.getId (), b.getId ());
    }
    
    public GraphDescriptor removeEdge (int a, int b) {
        final Vertex va = graph.getVertices ().get (a),
                     vb = graph.getVertices ().get (b);
        final Pair <Vertex, Vertex> pair = Pair.mp (va, vb);
   
        doRemoveEdge (pair); // If not oriented then two edges should be removed
        if (!graph.isOriented ()) { doRemoveEdge (pair.swap ()); }
        return this;
    }
    
    private void doRemoveEdge (Pair <Vertex, Vertex> pair) {
        edgeToChange.add (lmEA = graph.getEdges ().get (pair));
    }
    
    public boolean isConnected (boolean ignoreIfOneSingle, boolean trimIfOneSingle) {
        if (vertices.size () == 0) { return true; }
        
        Queue <Vertex> queue = new LinkedList <> ();
        queue.add (vertices.iterator ().next ());
        
        Set <Vertex> visited = new HashSet<> ();
        visited.add (queue.peek ());
        
        int iters = 100;
        while (!queue.isEmpty () && --iters >= 0) {
            Vertex vertex = queue.poll ();
            for (Edge edge : vertex.getEdges ().values ()) {
                boolean cont1 = edges.contains (edge), 
                        cont2 = edgeToChange.contains (edge);
                if ((!cont1 && !cont2) || (cont1 && cont2)) { 
                    continue; // this edge exactly not in graph
                }
                
                if (!visited.contains (edge.S)) {
                    visited.add (edge.S);
                    queue.add (edge.S);
                }
            }
        }
        
        Vertex notCovered = null;
        for (Vertex vertex : vertices) {
            if (visited.contains (vertex)) { continue; }
            
            if (vertexToChange.contains (vertex)) {
                continue; // This vertex will be removed
            }
            
            if (notCovered == null) {
                notCovered = vertex;
            } else { 
                // More than 1 not covered vertex
                return false;
            }
        }
        
        for (Vertex vertex : vertexToChange) {
            if (visited.contains (vertex)) { continue; }
            
            if (vertices.contains (vertex)) {
                continue; // This vertex will be removed
            }
            
            if (notCovered == null) {
                notCovered = vertex;
            } else { 
                // More than 1 not covered vertex
                return false;
            }
        }
        
        if (trimIfOneSingle && notCovered != null) {
            vertices.remove (notCovered);
        }
        
        // If it's really connected or it become connected after trimming
        return notCovered == null || ignoreIfOneSingle;
    }
    
    public List <Vertex> getVertices (boolean stable) {
        if (stable) { return new ArrayList <> (vertices); }
        
        Set <Vertex> result = new HashSet <> (vertices);
        vertexToChange.forEach (vertex -> {
            if (result.contains (vertex)) {
                result.remove (vertex);
            } else {
                result.add (vertex);
            }
        });
        
        return new ArrayList <> (result);
    }
    
    public List <Edge> getInnerEdges (boolean stable) {
        // for the graph without (edge|vertex)ToChange
        if (stable) { return new ArrayList <> (edges); }
        
        Set <Edge> result = new HashSet <> (edges);
        edgeToChange.forEach (edge -> {
            if (result.contains (edge)) {
                result.remove (edge);
            } else {
                result.add (edge);
            }
        });
        
        return new ArrayList <> (result);
    }
    
    public Set <Edge> getCloseEdges (boolean stable) {
        Set <Edge> edges = new HashSet <> ();
        for (Vertex vertex : getVertices (stable)) {
            edges.addAll (vertex.getEdges ().values ());
        }
        
        return edges;
    }
    
    public List <Edge> getOuterEdges (boolean stable) {
        Set <Edge> edges = getCloseEdges (stable);
        edges.removeAll (getInnerEdges (stable));
        return new ArrayList <> (edges);
    }
    
    public Edge selectRandomEdgeFromGraph () {
        int index = R.nextInt (graph.getEdges ().size ());
        for (Edge edge : graph.getEdges ().values ()) {
            if (index-- == 0) { return edge; }
        }
        
        return null; // impossible
    }
    
    public Edge selectRandomEdgeFromSunRays () {
        final List <Edge> edges = getOuterEdges (true);
        return edges.get (R.nextInt (edges.size ()));
    }
    
    public Edge selectRandomEdgeFromHedgehog () {
        List <Edge> edges = getInnerEdges (true);
        edges.addAll (getOuterEdges (true));
        
        return edges.get (R.nextInt (edges.size ()));
    }
    
    public Edge selectRandomEdgeFromPotato () {
        final List <Edge> edges = getInnerEdges (true);
        return edges.get (R.nextInt (edges.size ()));
    }
    
}
