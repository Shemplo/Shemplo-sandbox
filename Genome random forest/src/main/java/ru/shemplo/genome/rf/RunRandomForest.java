package ru.shemplo.genome.rf;

import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.shemplo.genome.rf.data.DataParser;
import ru.shemplo.genome.rf.data.EntityVerdict;
import ru.shemplo.genome.rf.data.NormalizedMatrix;
import ru.shemplo.genome.rf.data.SourceDataset;
import ru.shemplo.genome.rf.data.SourceEntity;
import ru.shemplo.genome.rf.forest.AvDistanceStrategy;
import ru.shemplo.genome.rf.forest.RandomForest;

public class RunRandomForest {
 
    public static void main (String ... args) throws Exception {
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
        RandomForest forest = new RandomForest (matrix, new AvDistanceStrategy (), dataset, 4, 121)
                            . train ();
        
        int correct = 0, total = 0;
        for (int i = 0; i < dataset.getSize (); i++, total++) {
            SourceEntity entity = dataset.getEntityByIndex (i);
            if (entity.getVerdict () == EntityVerdict.NEVUS) { total--; continue; }
            if (forest.predict (entity.getGenesExpMap ()).equals (entity.getVerdict ())) {
                correct += 1;
            }
        }
        System.out.println ("Corrrect: " + correct + " / " + total);
        System.out.println ("END");
    }
    
}
