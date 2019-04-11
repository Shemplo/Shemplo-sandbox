package ru.shemplo.metagennet.io;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import ru.shemplo.metagennet.graph.Edge;
import ru.shemplo.metagennet.graph.GraphDescriptor;
import ru.shemplo.metagennet.graph.Vertex;
import ru.shemplo.snowball.stuctures.Pair;

public class CommonWriter {
    
    public void saveMap (String filepath, String parameter, 
            Map <Vertex, Double> occurrences) {
        try (
            PrintWriter pw = new PrintWriter (filepath);
        ) {
            pw.println ("gene\tpval\t" + parameter);
            occurrences.entrySet ().stream ()
            . map     (Pair::fromMapEntry)
            . sorted  ((a, b) -> -Double.compare (a.S, b.S))
            . forEach (p -> {
                pw.println (String.format ("%9s\t%.12f\t%.6f", 
                     p.F.getName (), p.F.getWeight (), p.S));
            });
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        }
    }
    
    public void saveGradientDOTFile (String filepath, GraphDescriptor graph, 
            Map <Vertex, Double> occurrences, Color maxColor, Color minColor) {
        try (
            PrintWriter pw = new PrintWriter (filepath);
        ) {
            final Set <Vertex> vertices = graph.getVertices ();
            final Set <Edge> edges = graph.getEdges ();
            pw.println (makeGradientString (vertices, edges, 
                          maxColor, minColor, occurrences));
        } catch (IOException ioe) {
            ioe.printStackTrace ();
        };
    }
    
    private String makeGradientString (Set <Vertex> vertices, Set <Edge> edges,
            Color maxColor, Color minColor, Map <Vertex, Double> occurrences) {
        Map <Vertex, Double> map = new HashMap <> (occurrences);
        double max = occurrences.entrySet ().stream ()
                   . map         (Pair::fromMapEntry)
                   . mapToDouble (Pair::getS)
                   . max ().orElse (1);
        for (Vertex vertex : map.keySet ()) {
            map.compute (vertex, (__, v) -> v / max);
        }
        
        StringJoiner sj = new StringJoiner ("\n");
        sj.add ("graph prof {");
        //sj.add ("    rankdir=LR;");
        //sj.add ("    size=\"64\";");
        sj.add ("    overlap=false;");
        sj.add ("    splines=true;");
        sj.add ("    ratio=fill;");
        sj.add ("    node [style = filled];");
        for (Vertex vertex : vertices) {
            if (vertex == null) { continue; }
            
            final double weight = map.get (vertex);
            String appendix = makeGradientColor (weight, maxColor, minColor);
            sj.add (String.format ("    \"%s\" [color = \"%s\"];", 
                                    vertex.getName (), appendix));
        }
        
        sj.add ("    node [shape = circle];");
        sj.add ("    node [color = black];");
        
        for (Edge edge : edges) {
            if (edge.F.getId () > edge.S.getId ()) { continue; }
            
            String appendix = "";
            /*
            if (vertices.contains (edge.F) && vertices.contains (edge.S)
                    && edges.contains (edge)) {
                appendix = "[color = red]";
            }
            */
            sj.add (String.format ("    \"%s\" -- \"%s\" %s;", 
                         edge.F.getName (), edge.S.getName (),
                         appendix));
        }
        
        sj.add ("}");
        
        return sj.toString ();
    }
    
    private String makeGradientColor (double weight, Color maxColor, Color minColor) {
        int r = minColor.getRed   () + (int) ((maxColor.getRed   () - minColor.getRed   ()) * weight);
        int g = minColor.getGreen () + (int) ((maxColor.getGreen () - minColor.getGreen ()) * weight);
        int b = minColor.getBlue  () + (int) ((maxColor.getBlue  () - minColor.getBlue  ()) * weight);
        
        return String.format ("#%02x%02x%02x", r, g, b);
    }
    
}
