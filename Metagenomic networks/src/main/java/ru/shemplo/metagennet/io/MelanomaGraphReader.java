package ru.shemplo.metagennet.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.GraphModules;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;
import ru.shemplo.snowball.utils.fp.StreamUtils;

public class MelanomaGraphReader implements GraphReader {

    @Override
    public Graph readGraph () throws IOException {
        List <Pair <String, String>> edgesDesc = readEdges ();
        Map <String, Double> genesDesc = readGenes ();
        Graph graph = new Graph (false);
        
        edgesDesc = edgesDesc.stream ()
                  . filter  (pair -> genesDesc.containsKey (pair.F)
                                  && genesDesc.containsKey (pair.S))
                  . collect (Collectors.toList ());
        List <String> genesVerts = edgesDesc.stream ()
                                 . flatMap  (pair -> Stream.of (pair.F, pair.S))
                                 . distinct ().sorted ()
                                 . collect  (Collectors.toList ());
        Map <String, Integer> gene2index = new HashMap <> ();
        for (int i = 0; i < genesVerts.size (); i++) {
            String name = genesVerts.get (i);
            graph.addVertex (i, genesDesc.get (name));
            gene2index.put (name, i);
        }
        
        for (Pair <String, String> edge : edgesDesc) {
            graph.addEdge (gene2index.get (edge.F), gene2index.get (edge.S), 1.0);
        }
        
        graph.setModules (GraphModules.splitGraph (graph));
        return graph;
    }
    
    private List <Pair <String, String>> readEdges () throws IOException {
        List <Pair <String, String>> edges = new ArrayList <> ();
        Path filepath = Paths.get ("runtime/inwebIM_ppi.txt");
        try (
            BufferedReader br = Files.newBufferedReader (filepath);
        ) {
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final StringTokenizer st = new StringTokenizer (line);
                edges.add (Pair.mp (st.nextToken (), st.nextToken ()));
            }
        }
        
        return edges;
    }
    
    private Map <String, Double> readGenes () throws IOException {
        final Map <String, Double> genes = new HashMap <> ();
        Path filepath = Paths.get ("runtime/melanoma");
        try (
            BufferedReader br = Files.newBufferedReader (filepath);
        ) {
            br.readLine (); // titles
            
            String line = null;
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                final StringTokenizer st = new StringTokenizer (line);
                List <String> tokens = StreamUtils.whilst (StringTokenizer::hasMoreTokens, 
                                                           StringTokenizer::nextToken, st)
                                     . collect (Collectors.toList ());
                Double pvalue = Double.parseDouble (tokens.get (2));
                genes.put (tokens.get (1), pvalue);
            }
        }
        
        return genes;
    }
    
}
