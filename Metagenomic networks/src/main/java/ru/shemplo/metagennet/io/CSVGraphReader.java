package ru.shemplo.metagennet.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import ru.shemplo.metagennet.graph.Graph;
import ru.shemplo.metagennet.graph.Graph.Edge;
import ru.shemplo.metagennet.graph.Graph.Vertex;
import ru.shemplo.metagennet.graph.GraphModules;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.StringManip;

public class CSVGraphReader {
    
    public Graph readGraph () throws IOException {
        Pair <List <String>, List <List <String>>> verticesCSV = readCSV ("runtime/vertices.csv");
        Pair <List <String>, List <List <String>>> edgesCSV = readCSV ("runtime/edges.csv");
        
        AtomicInteger counter = new AtomicInteger ();
        final Map <String, Vertex> vertices 
            = verticesCSV.S.stream ().map (lst -> Pair.mp (lst.get (1), lst))
            . map     (p -> p.applyS (lst -> {
                final int index = counter.getAndIncrement ();
                Double weight = null;
                try   { weight = Double.parseDouble (lst.get (6).replace (',', '.')); } 
                catch (NumberFormatException nfe) { weight = 1D; }
                
                return new Vertex (index, weight);
            }))
            . collect (Collectors.toMap (Pair::getF, Pair::getS));
        
        Graph graph = new Graph (new HashMap <> (), new GraphModules (), new HashSet <> ());
        final AtomicReference <Graph> ref = new AtomicReference <> (graph);
        
        edgesCSV.S.forEach (edge -> {
            String from = edge.get (1), to = edge.get (2);
            Double weight = null;
            
            try   { weight = Double.parseDouble (edge.get (4).replace (',', '.')); } 
            catch (NumberFormatException nfe) { weight = 1D; }
            
            Edge edgeI = new Edge (vertices.get (from), vertices.get (to), weight);
            ref.set (ref.get ().addEdges (false, edgeI, edgeI.swap ()));
        });
        
        ref.get ().setInitial (true);
        return ref.get ();
    }
    
    private Pair <List <String>, List <List <String>>> readCSV (String filepath) throws IOException {
        final Path path = Paths.get (filepath);
        
        try (
            BufferedReader br = Files.newBufferedReader (path);
        ) {
            List <String> titles = Arrays.asList (br.readLine ().split (";"));
            
            String line = null;
            List <List <String>> rows = new ArrayList <> ();
            while ((line = StringManip.fetchNonEmptyLine (br)) != null) {
                rows.add (Arrays.asList (line.split (";")));
            }
            
            return Pair.mp (titles, rows);
        }
    }
    
}
