package ru.shemplo.genome.rf;

import java.util.Map;

import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.shemplo.genome.rf.data.DataParser;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.forest.AvDistanceStrategy;
import ru.shemplo.genome.rf.forest.RandomForest;
import ru.shemplo.genome.rf.forest.SplitStrategy;
import ru.shemplo.snowball.stuctures.Pair;

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
        
        NormalizedMatrix matrix = dataset.getNormalizedMatrix ();
        SplitStrategy strategy = new AvDistanceStrategy ();
        RandomForest forest = new RandomForest (matrix, strategy, dataset, 10, 301)
                            . train ();
        
        System.out.println ("[] Probabilities: ");
        Map <String, Double> probs = forest.makeProbabilities ();
        probs.keySet ().stream ()
                       . map (k -> Pair.mp (k, probs.get (k)))
                       . sorted ((pa, pb) -> -Double.compare (pa.S, pb.S))
                       . map (p -> String.format (" - %24s %.16f", p.F, p.S))
                       . forEach (System.out::println);
        
        long end = System.currentTimeMillis ();
        System.out.println (String.format ("[] Done (running time %d ms)", end - start));
    }
    
}
