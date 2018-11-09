package ru.shemplo.genome.rf;

import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ru.shemplo.genome.rf.data.DataParser;
import ru.shemplo.genome.rf.data.SourceDataset;

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
        
        dataset.getNormalizedMatrix ().getShuffledMatrix ();
        System.out.println ("END");
    }
    
}
