package ru.shemplo.metagennet.graph;

import java.util.*;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.shemplo.metagennet.graph.GraphModules.GraphModule;
import ru.shemplo.snowball.stuctures.Trio;

@RequiredArgsConstructor (access = AccessLevel.PACKAGE)
public class GraphDescriptor implements Cloneable {

    private final Random R = new Random ();
    private final double betaAV, betaAE;
    
    private final boolean signal;
    private final Graph graph;
    
    private final Deque <Trio <Integer, Edge, Double>> history 
          = new LinkedList <> ();
    
    @Getter private final Map <GraphModule, Set <Vertex>> modules = new HashMap <> ();
    @Getter private final Set <Vertex> vertices = new HashSet <> ();
    @Getter private final Set <Edge> edges  = new HashSet <> (),
                                     bedges = new HashSet <> ();
    
    private double likelihood = 1;
    
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
        
        for (Edge edge : graph.getEdgesList ()) {
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
    
    public GraphDescriptor commit () {
        history.clear (); return this;
    }
    
    public GraphDescriptor rollback () {
        while (!history.isEmpty ()) {
            Trio <Integer, Edge, Double> event = history.pollLast ();
            if (event.F == 0) {
                applyRemove (event.S);
            } else if (event.F == 1) {
                applyAdd (event.S);
            }
            
            likelihood = event.T;
        }
        
        return this;
    }
    
    public GraphDescriptor addEdge (Edge edge) {
        history.add (Trio.mt (0, edge, likelihood));
        applyAdd (edge);
        return this;
    }
    
    private void applyAdd (Edge edge) {
        edges.add (edge);
        
        if (!signal) {
            final double w = edge.getWeight ();
            likelihood = betaAE * Math.pow (w, betaAE);            
        }
        
        applyVertexAdd (edge.F);
        applyVertexAdd (edge.S);
        bedges.removeAll (edges);
    }
    
    private void applyVertexAdd (Vertex vertex) {
        if (!vertices.contains (vertex)) {
            vertices.add (vertex);
            applyVertex (vertex, 0);
            
            if (!signal) {
                final double w = vertex.getWeight ();
                likelihood *= betaAV * Math.pow (w, betaAV);                
            } else {
                GraphModule module = graph.getModules ().getModule (vertex);
                if (!modules.containsKey (module)) {
                    modules.put (module, new HashSet <> ());
                    
                    final double w = vertex.getWeight ();                
                    likelihood *= betaAV * Math.pow (w, betaAV);
                }
                
                modules.get (module).add (vertex);
            }
        }
    }
    
    public GraphDescriptor removeEdge (Edge edge) {
        history.add (Trio.mt (1, edge, likelihood));
        applyRemove (edge);
        return this;
    }
    
    private void applyRemove (Edge edge) {
        edges.remove (edge);
        
        if (!signal) {
            final double w = edge.S.getWeight ();            
            likelihood = betaAV * Math.pow (w, betaAV);
        }
        
        applyVertex (edge.F, 1);
        applyVertex (edge.S, 1);
        bedges.removeAll (edges);
    }
    
    private void applyVertex (Vertex vertex, int action) {
        int neis = 0;
        for (Edge nei : vertex.getEdges ().values ()) {
            Vertex vnei = nei.F.getId () == vertex.getId () 
                        ? nei.S : nei.F;
            if (vertices.contains (vnei)) {
                if   (action == 0) { bedges.remove (nei); } 
                else               { bedges.add (nei);    }
                
                neis += edges.contains (nei) ? 1 : 0;
            } else {
                if   (action == 0) { bedges.add (nei);    } 
                else               { bedges.remove (nei); }
            }
        }
        
        //System.out.println (vertex + " " + neis);
        if (action == 1 && neis == 0) { 
            if (!signal) {
                final double w = vertex.getWeight ();
                likelihood *= betaAV * Math.pow (w, betaAV);
            } else {
                GraphModule module = graph.getModules ().getModule (vertex);
                Set <Vertex> set = modules.get (module);
                set.remove (vertex);
                
                if (set.isEmpty ()) {
                    modules.remove (module);
                    
                    final double w = vertex.getWeight ();
                    likelihood /= betaAV * Math.pow (w, betaAV);
                }
            }
            
            vertices.remove (vertex); 
        }
    }
    
    public boolean isConnected () {
        if (vertices.size () == 0) { return true; }
        
        final Queue <Vertex> queue = new LinkedList <> ();
        queue.add (vertices.iterator ().next ());
        
        Set <Integer> visited = new HashSet<> ();
        visited.add (queue.peek ().getId ());
        
        int iters = 10000;
        while (!queue.isEmpty () && --iters >= 0) {
            Vertex vertex = queue.poll ();
            for (Edge edge : vertex.getEdges ().values ()) {
                if (!edges.contains (edge)) { continue; }
                
                Vertex vnei = edge.F.getId () == vertex.getId () 
                            ? edge.S : edge.F;
                if (!visited.contains (vnei.getId ())) {
                    visited.add (vnei.getId ());
                    queue.add (vnei);
                }
            }
        }
        //System.out.println ("Iters: " + iters);
        
        return vertices.size () == visited.size ();
    } 
    
    public Edge getRandomInnerEdge () {
        int index = R.nextInt (edges.size ());
        for (Edge edge : edges) {
            if (index-- == 0) { return edge; }
        }
        
        return null;
    }
    
    public int getInnerEdges () {
        return edges.size ();
    }
    
    public Edge getRandomBorderEdge () {
        int index = R.nextInt (bedges.size ());
        for (Edge edge : bedges) {
            if (index-- == 0) { return edge; }
        }
        
        return null;
    }
    
    public int getBorderEdges () {
        return bedges.size ();
    }
    
    public Edge getRandomBorderOrInnerEdge () {
        int index = R.nextInt (bedges.size () + edges.size ());
        if (index < bedges.size ()) {
            for (Edge edge : bedges) {
                if (index-- == 0) { return edge; }
            }
        } else {
            index -= bedges.size ();
            for (Edge edge : edges) {
                if (index-- == 0) { return edge; }
            }
        }
        
        return null;
    }
    
    public double getLikelihood () {
        return likelihood;
    }
    
    /*
    @SuppressWarnings ("unused")
    private final boolean signal;
    private final Graph graph;
    
    @Getter private final Set <Integer> 
        vertices    = new HashSet <> ();
    @Getter private final Set <Edge> 
        edges       = new HashSet <> ();
    private Set <Edge> borderEdges = new HashSet <> ();
    
    private Edge tmpEdgeAdd = null, tmpEdgeRemove = null;
    
    private boolean isAC (Edge edge) { // Add candidate
        return edge.equals (tmpEdgeAdd);
    }
    
    private boolean isRMC (Edge edge) { // Remove candidate
        return edge.equals (tmpEdgeRemove);
    }
    
    private boolean isActiveEdge (Edge edge) {
        final boolean now = edges.contains (edge);
        return (now && !isRMC (edge)) || (!now && isAC (edge));
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
        final GraphModules modules = graph.getModules ();
        Set <GraphModule> modulesV = new HashSet <> ();
        double result = 1.0d;
        
        return result;
    }
    
    public GraphDescriptor commit () {
        rollback (); // to clear all
        return this;
    }
    
    public GraphDescriptor rollback () {
        tmpEdgeRemove = null;
        tmpEdgeAdd = null; 
        return this;
    }
    
    public GraphDescriptor addEdge (Edge edge) {
        tmpEdgeAdd = edge; return this;
    }
    
    public GraphDescriptor removeEdge (Edge edge) {
        tmpEdgeRemove = edge; return this;
    }
    
    public boolean isConnected (boolean ignoreIfOneSingle, boolean trimIfOneSingle) {
        if (vertices.size () == 0 || (tmpEdgeAdd == null && tmpEdgeRemove == null)) { 
            return true; 
        }
        
        if (tmpEdgeAdd != null && tmpEdgeRemove == null) {
            return vertices.contains (tmpEdgeAdd.F.getId ())
                || vertices.contains (tmpEdgeAdd.S.getId ());
        } else if (tmpEdgeRemove != null) {
            final Queue <Vertex> queue = new LinkedList <> ();
            queue.add (tmpEdgeRemove.F);
            
            Set <Integer> visited = new HashSet<> ();
            visited.add (queue.peek ().getId ());
            
            int iters = 10000;
            while (!queue.isEmpty () && --iters >= 0) {
                Vertex vertex = queue.poll ();
                for (Edge edge : vertex.getEdges ().values ()) {
                    if (!isActiveEdge (edge)) { continue; }
                    
                    if (!visited.contains (edge.S.getId ())) {
                        visited.add (edge.S.getId ());
                        queue.add (edge.S);
                    }
                }
            }
            
            int dist  = Math.abs (vertices.size () - visited.size ());
            int limit = ignoreIfOneSingle ? 1 : 0;
            if (dist > limit) { return false; }
            
            if (dist == 1 && trimIfOneSingle) {
                for (Integer vertexID : vertices) {
                    if (!visited.contains (vertexID)) {
                        tmpTrimVertex1 = vertexID;
                        break;
                    }
                }
                
                return ignoreIfOneSingle;
            }
            
            return true;
        }
        
        return false;
    }
    
    public Set <Integer> getVertices (boolean stable) {
        if (stable) { return vertices; }
        
        Set <Integer> result = new HashSet <> (vertices);
        if (!stable && tmpTrimVertex1 != null) {
            result.remove (tmpTrimVertex1);
        }
        if (!stable && tmpTrimVertex2 != null) {
            result.remove (tmpTrimVertex2);
        }
        
        return result;
    }
    
    public List <Vertex> getVerticesOrdered (boolean stable) {
        final List <Vertex> verts = new ArrayList <> ();
        for (Integer vertexID : getVertices (stable)) {
            verts.add (graph.getVertices ().get (vertexID));
        }
        
        return verts;
    }
    
    public List <Edge> getInnerEdges (boolean stable) {
        // for the graph without (edge|vertex)ToChange
        if (stable) { return new ArrayList <> (edges); }
        
        Set <Edge> result = new HashSet <> (edges);
        result.remove (tmpEdgeRemove);
        result.add (tmpEdgeAdd); 
        
        return new ArrayList <> (result);
    }
    
    public Set <Edge> getBorderEdges (boolean stable) {
        if (stable) { return borderEdges; }
        
        Set <Edge> edges = new HashSet <> (borderEdges);
        if (!stable && tmpTrimVertex1 != null) {
            Vertex v = graph.getVertices ().get (tmpAddVertex1);
            edges.removeAll (v.getEdges ().values ());
            edges.add (tmpEdgeRemove);
        }
        if (!stable && tmpTrimVertex2 != null) {
            Vertex v = graph.getVertices ().get (tmpAddVertex2);
            edges.removeAll (v.getEdges ().values ());
            edges.add (tmpEdgeRemove);
        }
        
        if (tmpAddVertex1 != null) {
            Vertex v = graph.getVertices ().get (tmpAddVertex1);
            edges.addAll (v.getEdges ().values ());
            edges.remove (tmpEdgeAdd);
        }
        if (tmpAddVertex2 != null) {
            Vertex v = graph.getVertices ().get (tmpAddVertex2);
            edges.addAll (v.getEdges ().values ());
            edges.remove (tmpEdgeAdd);
        }
        
        edges.removeAll (edges);
        return edges;
    }
    
    public Edge selectRandomEdgeFromGraph () {
        int index = R.nextInt (graph.getEdgesList ().size ());
        return graph.getEdgesList ().get (index);
    }
    
    public Edge selectRandomEdgeFromSunRays () {
        int index = R.nextInt (borderEdges.size ());
        for (Edge edge : borderEdges) {
            if (index-- == 0) { return edge; }
        }
        
        return null; // impossible
    }
    
    public Edge selectRandomEdgeFromBorderAndInside () {
        int index = R.nextInt (borderEdges.size () + edges.size ());
        if (index < borderEdges.size ()) {
            for (Edge edge : borderEdges) {
                if (index-- == 0) { return edge; }
            }
        } else {
            index -= borderEdges.size ();
            for (Edge edge : edges) {
                if (index-- == 0) { return edge; }
            }
        }
        
        return null; // impossible
    }
    
    public Edge selectRandomEdgeFromInside () {
        int index = R.nextInt (edges.size ());
        for (Edge edge : edges) {
            if (index-- == 0) { return edge; }
        }
        
        return null; // impossible
    }
    */
    
}
