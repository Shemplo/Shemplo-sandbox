package ru.shemplo.genome.rf;

import static java.util.Arrays.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.shemplo.genome.rf.data.DataParser;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.genome.rf.forest.AvDistanceStrategy;
import ru.shemplo.genome.rf.forest.RandomForest;
import ru.shemplo.genome.rf.forest.SplitStrategy;
import ru.shemplo.snowball.stuctures.Pair;
import ru.shemplo.snowball.utils.fun.StreamUtils;

public class RunRandomForest {
 
    public static void main (String ... args) throws Exception {
        long start = System.currentTimeMillis ();
        if (args.length == 0) {
            String message = "Missed argument [name of file]";
            throw new IllegalStateException (message);
        }
        
        Path path = Paths.get (args [0]);
        if (!Files.exists (path)) {
            String message = "File `" + path + "` not found";
            throw new IllegalStateException (message);
        }
        
        SourceDataset dataset = null;
        try (                
            InputStream is = Files.newInputStream (path);
        ) {
            dataset = new DataParser ().parse (is);
        }
        
        final Map <String, String> decodedNames = new HashMap <> ();
        String baseFile = "/de.csv";
        try (
            InputStream is = RunRandomForest.class.getResourceAsStream (baseFile);
            Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            br.readLine (); // Skip titles line
            
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.length () == 0) { continue; }
                
                List <String> tokens = Arrays.asList (line.split ("\",\""));
                decodedNames.put (tokens.get (2), tokens.get (4)); 
                //                           ID / Gene.symbol
            }
        }
        
        Set <String> mentionedNames = new HashSet <> ();
        baseFile = "/interactions.txt";
        try (
            InputStream is = RunRandomForest.class.getResourceAsStream (baseFile);
            Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader (r);
        ) {
            String line = null;
            while ((line = br.readLine ()) != null) {
                if (line.length () == 0) { continue; }
                
                List <String> tokens = StreamUtils
                                     . whilst (StringTokenizer::hasMoreTokens, 
                                               StringTokenizer::nextToken, 
                                               new StringTokenizer (line))
                                     . collect (Collectors.toList ());
                if (tokens.get (0).equals (tokens.get (3))) { continue; }
                mentionedNames.addAll (asList (tokens.get (0), tokens.get (3)));
            }
        }
        
        Map <String, String> restrictedNames 
            = decodedNames.entrySet ().stream ().map (Pair::fromMapEntry)
            . filter (p -> mentionedNames.contains (p.S))
            . collect (Collectors.toMap (p -> p.F, p -> p.S));
        
        for (int i = 0; i < dataset.getSize (); i++) {
            dataset.updateEntity (i, e -> e.restrictGenes (restrictedNames));
        }
        
        NormalizedMatrix matrix = dataset.getNormalizedMatrix ();
        SplitStrategy strategy = new AvDistanceStrategy ();
        RandomForest forest = new RandomForest (matrix, strategy, dataset, 20, 240)
                            . train ();
        
        System.out.println ("[] Probabilities: ");
        Map <String, Double> probs = forest.makeProbabilities ();
        probs.keySet ().stream ()
                       . map (k -> Pair.mp (k, probs.get (k)))
                       . sorted ((pa, pb) -> -Double.compare (pa.S, pb.S))
                       . map (p -> String.format (" - %32s ~ %.1f", p.F, p.S))
                       . forEach (System.out::println);
        
        long end = System.currentTimeMillis ();
        System.out.println (String.format ("[] Done (running time %d ms)", end - start));
        System.out.println (">> Additional part running");
        
        List <SourceEntity> list = matrix.getEntitiesName ().stream ()
                                 . map (dataset::getEntityByGeoAccess)
                                 . collect (Collectors.toList ());
        int index = matrix.getRowOfGene ("KRT18");
        System.out.println (strategy.getDifference (list, matrix, index) [0]);
    }
    
}
