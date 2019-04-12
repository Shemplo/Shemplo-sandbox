package ru.shemplo.metagennet.io;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Edge;
import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.GraphSignals;
import ru.shemplo.metagennet.graph.Vertex;
import ru.shemplo.snowball.stuctures.Pair;

public class GWASGraphReader extends CSVGraphReader {
    
    @Override
    public Graph readGraph (String filenamePrefix) throws IOException {
        Pair <List <String>, List <List <String>>> edgesCSV = readCSV ("runtime/" + filenamePrefix + "edges.csv");
        Pair <List <String>, List <List <String>>> verticesCSV = readCSV ("runtime/gwas.csv");
        
        AtomicInteger counter = new AtomicInteger ();
        final Map <String, Vertex> vertices 
            = verticesCSV.S.stream ().map (lst -> Pair.mp (lst.get (0), lst))
            . map     (p -> p.applyS (lst -> {
                final int index = counter.getAndIncrement ();
                Double weight = null;
                try   { weight = Double.parseDouble (lst.get (1).replace (',', '.')); } 
                catch (NumberFormatException nfe) { weight = 1D; }
                
                Vertex vertex = new Vertex (index, weight);
                vertex.setName (p.F.replace ("\"", ""));
                return vertex;
            }))
            . collect (Collectors.toMap (Pair::getF, Pair::getS));
        
        int fromColumnE = edgesCSV.F.indexOf ("\"from\""),
            toColumnR   = edgesCSV.F.indexOf ("\"to\"");
        int pvalColumnE = edgesCSV.F.indexOf ("\"weight\"") != -1 
                        ? edgesCSV.F.indexOf ("\"weight\"")
                        : edgesCSV.F.indexOf ("\"pval\"");
        Graph graph = new Graph (0.571, 1.0);
        edgesCSV.S.forEach (edge -> {
            String from = edge.get (fromColumnE).replaceAll ("\\(\\d+\\)", "").replace ("\"", ""), 
                   to   = edge.get (toColumnR).replaceAll ("\\(\\d+\\)", "").replace ("\"", "");
            Double weight = null;
            
            try   { weight = Double.parseDouble (edge.get (pvalColumnE).replace (',', '.')); } 
            catch (NumberFormatException nfe) { weight = 1D; }
            
            if (!vertices.containsKey (from) || !vertices.containsKey (to)) { return; }
            Edge edgeI = new Edge (vertices.get (from), vertices.get (to), weight);
            graph.addEdge (edgeI);
        });
        
        graph.setSignals (GraphSignals.splitGraph (graph));
        return graph;
    }
    
}
